package com.ventureai.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Portfolio entry — one row per ACCEPTED collaboration.
 * Explicitly does NOT include any financial investment fields.
 */
public record PortfolioStartupResponse(
        Long collaborationRequestId,
        LocalDateTime collaborationAcceptedAt,
        Long startupId,
        String companyName,
        String description,
        String industry,
        String startupStage,
        String businessModel,
        Integer teamSize,
        Long totalUsers,
        Long customerCount,
        BigDecimal monthlyRevenue,
        BigDecimal revenueGrowthRate,
        BigDecimal totalFunding,
        BigDecimal valuation,
        String website
) {}