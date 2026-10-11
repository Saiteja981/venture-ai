package com.ventureai.backend.controller;

import com.ventureai.backend.dto.AiFeedbackResponse;
import com.ventureai.backend.dto.SubmitFeedbackRequest;
import com.ventureai.backend.service.AiFeedbackService;
import com.ventureai.backend.service.AiProxyService;
import com.ventureai.backend.service.ProxyResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Backend-owned proxy for AI-service calls.*
 * Using this path (instead of hitting FastAPI directly from the browser) lets
 * the backend record trusted execution metadata — userId from JWT, real
 * latency, outcome — and issue the executionId that feedback links to.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiProxyService proxyService;
    private final AiFeedbackService feedbackService;

    public AiController(AiProxyService proxyService,
                        AiFeedbackService feedbackService) {
        this.proxyService = proxyService;
        this.feedbackService = feedbackService;
    }

    /* ---------- Proxied AI endpoints ---------- */

    @PostMapping("/predict-startup")
    public ResponseEntity<Map<String, Object>> predictStartup(
            @RequestParam(required = false) Boolean explain,
            @RequestBody Map<String, Object> body) {
        ProxyResult result = proxyService.predictStartup(body, Boolean.TRUE.equals(explain));
        return ResponseEntity.ok(result.body());
    }

    @PostMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> recommendForInvestor(
            @RequestParam(required = false) Boolean explain,
            @RequestBody Map<String, Object> body) {
        ProxyResult result = proxyService.recommendStartupsForInvestor(
                body, Boolean.TRUE.equals(explain));
        return ResponseEntity.ok(result.body());
    }

    @PostMapping("/investor-matches")
    public ResponseEntity<Map<String, Object>> recommendForStartup(
            @RequestParam(required = false) Boolean explain,
            @RequestBody Map<String, Object> body) {
        ProxyResult result = proxyService.recommendInvestorsForStartup(
                body, Boolean.TRUE.equals(explain));
        return ResponseEntity.ok(result.body());
    }

    @PostMapping("/analyze-pitch-deck")
    public ResponseEntity<Map<String, Object>> analyzePitchDeck(
            @RequestBody Map<String, Object> body) {
        ProxyResult result = proxyService.analyzePitchDeck(body);
        return ResponseEntity.ok(result.body());
    }
    /* ---------- Feedback ---------- */

    @PostMapping("/executions/{id}/feedback")
    public ResponseEntity<AiFeedbackResponse> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.submit(id, request));
    }

    @GetMapping("/executions/{id}/feedback")
    public ResponseEntity<AiFeedbackResponse> getMyFeedback(@PathVariable Long id) {
        AiFeedbackResponse response = feedbackService.getMine(id);
        return response == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(response);
    }
}