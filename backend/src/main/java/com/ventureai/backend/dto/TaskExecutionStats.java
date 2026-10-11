package com.ventureai.backend.dto;

public record TaskExecutionStats(
        String task,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        double successRatePercent,
        Double avgLatencyMs,
        Long maxLatencyMs
) {}