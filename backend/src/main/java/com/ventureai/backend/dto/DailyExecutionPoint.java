package com.ventureai.backend.dto;

public record DailyExecutionPoint(
        String date,               // ISO yyyy-MM-dd
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions
) {}