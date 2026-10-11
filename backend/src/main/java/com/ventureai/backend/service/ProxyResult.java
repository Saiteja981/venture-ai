package com.ventureai.backend.service;

import com.ventureai.backend.entity.AiExecutionOutcome;

import java.util.Map;

public record ProxyResult(
        Map<String, Object> body,
        Long executionId,
        long latencyMs,
        AiExecutionOutcome outcome
) {}