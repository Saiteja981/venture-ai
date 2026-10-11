package com.ventureai.backend.dto;

import java.util.Map;

public record AdminStatsResponse(
        long totalUsers,
        long totalStartups,
        long totalInvestors,
        long totalAdmins,
        long totalAiExecutions,
        Map<String, Long> usersByRole,
        Map<String, Long> startupsByIndustry,
        Map<String, Long> startupsByStage,
        Map<String, Long> investorsByType
) {}