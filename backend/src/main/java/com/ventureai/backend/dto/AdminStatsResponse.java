package com.ventureai.backend.dto;

import java.util.Map;

public record AdminStatsResponse(
        long totalUsers,
        long totalStartups,
        long totalInvestors,
        long totalAdmins,
        Map<String, Long> usersByRole,          // e.g. {"STARTUP": 12, "INVESTOR": 5, "ADMIN": 1}
        Map<String, Long> startupsByIndustry,   // e.g. {"FinTech": 4, "SaaS": 3, ...}
        Map<String, Long> startupsByStage,      // e.g. {"PRE_SEED": 6, "SEED": 4, ...}
        Map<String, Long> investorsByType       // e.g. {"VC_FIRM": 3, "ANGEL": 2, ...}
) {}