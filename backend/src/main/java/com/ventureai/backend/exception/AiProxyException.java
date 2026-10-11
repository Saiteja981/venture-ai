package com.ventureai.backend.exception;

import java.util.Map;

/**
 * Wraps a failed proxied call to the AI service. Carries the upstream
 * HTTP status, the sanitized response body, and the audit information
 * for the failed execution (id + latency), so the client can still
 * receive the execution metadata and submit feedback if applicable.
 */
public class AiProxyException extends RuntimeException {

    private final int status;
    private final Map<String, Object> body;
    private final Long executionId;
    private final long latencyMs;

    public AiProxyException(int status, Map<String, Object> body,
                            Long executionId, long latencyMs) {
        super("AI proxy failure: HTTP " + status);
        this.status = status;
        this.body = body;
        this.executionId = executionId;
        this.latencyMs = latencyMs;
    }

    public int getStatus() { return status; }
    public Map<String, Object> getBody() { return body; }
    public Long getExecutionId() { return executionId; }
    public long getLatencyMs() { return latencyMs; }
}