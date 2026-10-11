package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Startup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe admin-facing view of a startup record. Omits owner credentials and
 * any fields that could expose private user data.
 */
public record AdminStartupSummaryResponse(
        Long id,
        String companyName,
        String industry,
        String startupStage,
        String businessModel,
        Integer teamSize,
        Long totalUsers,
        BigDecimal monthlyRevenue,
        BigDecimal totalFunding,
        BigDecimal valuation,
        LocalDateTime createdAt
) {
    public static AdminStartupSummaryResponse from(Startup s) {
        return new AdminStartupSummaryResponse(
                s.getId(),
                s.getCompanyName(),
                s.getIndustry(),
                s.getStartupStage(),
                s.getBusinessModel(),
                s.getTeamSize(),
                s.getTotalUsers(),
                s.getMonthlyRevenue(),
                s.getTotalFunding(),
                s.getValuation(),
                s.getCreatedAt()
        );
    }
}