package com.ventureai.backend.service;

import com.ventureai.backend.entity.*;
import com.ventureai.backend.exception.AiProxyException;
import com.ventureai.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxies AI-service calls through the backend so every AI execution can be
 * recorded with a trusted userId, real latency, and outcome. The proxied
 * response has an extra "_execution" key with the executionId so the frontend
 * can attach feedback to the specific call.
 *
 * Monitoring writes are wrapped in try/catch and never fail the AI request.
 */
@Service
public class AiProxyService {

    private static final Logger log = LoggerFactory.getLogger(AiProxyService.class);

    private final RestClient restClient;
    private final UserRepository userRepository;
    private final AiExecutionService executionService;

    public AiProxyService(RestClient.Builder builder,
                          @Value("${ai.service.base-url}") String baseUrl,
                          @Value("${ai.service.connect-timeout-ms:5000}") long connectTimeoutMs,
                          @Value("${ai.service.read-timeout-ms:60000}") long readTimeoutMs,
                          UserRepository userRepository,
                          AiExecutionService executionService) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
        this.userRepository = userRepository;
        this.executionService = executionService;
    }

    /* ---------- Public proxy methods ---------- */

    public ProxyResult predictStartup(Map<String, Object> body, boolean explain) {
        String path = explain
                ? "/api/v1/predict/startup?explain=true"
                : "/api/v1/predict/startup";
        return proxy(AiTaskType.STARTUP_PREDICTION, path, body);
    }

    public ProxyResult recommendStartupsForInvestor(Map<String, Object> body, boolean explain) {
        String path = explain
                ? "/api/v1/recommendations?explain=true"
                : "/api/v1/recommendations";
        return proxy(AiTaskType.RECOMMENDATION, path, body);
    }

    public ProxyResult recommendInvestorsForStartup(Map<String, Object> body, boolean explain) {
        String path = explain
                ? "/api/v1/recommendations/for-startup?explain=true"
                : "/api/v1/recommendations/for-startup";
        return proxy(AiTaskType.INVESTOR_MATCHES, path, body);
    }

    public ProxyResult analyzePitchDeck(Map<String, Object> body) {
        return proxy(AiTaskType.PITCH_DECK_ANALYSIS, "/api/v1/analyze/pitch-deck", body);
    }
    /* ---------- Core proxy path ---------- */

    private ProxyResult proxy(AiTaskType task, String path, Map<String, Object> body) {
        User user = tryCurrentUser();
        long startNanos = System.nanoTime();

        try {
            Map<String, Object> response = restClient.post()
                    .uri(path)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null) response = new HashMap<>();

            long latencyMs = elapsedMs(startNanos);
            String modelVersion = extractModelVersion(response);
            Long execId = tryRecord(user, task, modelVersion,
                    AiExecutionOutcome.SUCCESS, latencyMs, null);

            if (execId != null) {
                response.put("_execution", Map.of(
                        "id", execId,
                        "latencyMs", latencyMs,
                        "outcome", "SUCCESS"
                ));
            }

            return new ProxyResult(response, execId, latencyMs, AiExecutionOutcome.SUCCESS);

        } catch (RestClientResponseException upstreamError) {
            long latencyMs = elapsedMs(startNanos);
            int status = upstreamError.getStatusCode().value();
            AiErrorCategory category = classifyResponse(status);
            Long execId = tryRecord(user, task, null,
                    AiExecutionOutcome.FAILED, latencyMs, category);

            log.warn("AI service returned {} for task {}", status, task);

            throw new AiProxyException(status,
                    sanitizeErrorBody(status),
                    execId, latencyMs);

        } catch (ResourceAccessException networkError) {
            long latencyMs = elapsedMs(startNanos);
            AiErrorCategory category = classifyNetwork(networkError);
            Long execId = tryRecord(user, task, null,
                    AiExecutionOutcome.FAILED, latencyMs, category);

            log.warn("AI service unreachable for task {}: {}", task, networkError.getMessage());

            throw new AiProxyException(502,
                    Map.of("error", "AI service unavailable",
                            "message", "The AI service could not be reached."),
                    execId, latencyMs);

        } catch (AiProxyException e) {
            throw e;
        } catch (Exception unexpected) {
            long latencyMs = elapsedMs(startNanos);
            Long execId = tryRecord(user, task, null,
                    AiExecutionOutcome.FAILED, latencyMs, AiErrorCategory.INTERNAL);

            log.error("Unexpected error proxying AI task {}", task, unexpected);

            throw new AiProxyException(500,
                    Map.of("error", "AI proxy error",
                            "message", "An unexpected error occurred."),
                    execId, latencyMs);
        }
    }

    /* ---------- Failure-safe recording ---------- */

    private Long tryRecord(User user, AiTaskType task, String modelVersion,
                           AiExecutionOutcome outcome, long latencyMs,
                           AiErrorCategory errorCategory) {
        try {
            return executionService.record(user, task, modelVersion,
                    outcome, latencyMs, errorCategory);
        } catch (Exception e) {
            // Monitoring write failures MUST NOT break the AI request.
            log.warn("Failed to persist AI execution record for task {}: {}",
                    task, e.getMessage());
            return null;
        }
    }

    /* ---------- Classification + sanitization ---------- */

    private AiErrorCategory classifyResponse(int status) {
        if (status >= 400 && status < 500) return AiErrorCategory.VALIDATION;
        if (status == 503) return AiErrorCategory.MODEL_UNAVAILABLE;
        if (status >= 500) return AiErrorCategory.UPSTREAM_ERROR;
        return AiErrorCategory.INTERNAL;
    }

    private AiErrorCategory classifyNetwork(ResourceAccessException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        if (msg.contains("timed out") || msg.contains("timeout")) {
            return AiErrorCategory.TIMEOUT;
        }
        return AiErrorCategory.NETWORK;
    }

    /** Never echo raw stack traces or upstream internals to the client. */
    private Map<String, Object> sanitizeErrorBody(int upstreamStatus) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "AI service error");
        if (upstreamStatus == 503) {
            body.put("message", "The AI model is currently unavailable.");
        } else if (upstreamStatus >= 400 && upstreamStatus < 500) {
            body.put("message", "The AI request was rejected. Please check your input.");
        } else {
            body.put("message", "The AI service returned an error.");
        }
        return body;
    }

    private String extractModelVersion(Map<String, Object> response) {
        Object mv = response.get("model_version");
        return mv instanceof String ? (String) mv : null;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private User tryCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}