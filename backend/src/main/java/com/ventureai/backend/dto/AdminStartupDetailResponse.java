package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Startup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Full detail view for an individual startup, minus the owner's credentials.
 *
 * Note: derived AI features (revenuePerCustomer, usersPerTeamMember,
 * growthAdjustedRunway, fundingToRevenue, burnToRevenue) are computed
 * on-the-fly by the AI service and are not stored on the Startup entity,
 * so they are intentionally not exposed here.
 */
public record AdminStartupDetailResponse(
        Long id,
        String companyName,
        String description,
        String industry,
        String startupStage,
        String businessModel,
        String website,
        Integer teamSize,
        Long totalUsers,
        Long customerCount,
        BigDecimal monthlyRevenue,
        BigDecimal revenueGrowthRate,
        BigDecimal totalFunding,
        BigDecimal monthlyBurnRate,
        BigDecimal runwayMonths,
        BigDecimal valuation,
        Long ownerId,
        String ownerName,
        String ownerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminStartupDetailResponse from(Startup s) {
        var owner = s.getUser();
        return new AdminStartupDetailResponse(
                s.getId(),
                s.getCompanyName(),
                s.getDescription(),
                s.getIndustry(),
                s.getStartupStage(),
                s.getBusinessModel(),
                s.getWebsite(),
                s.getTeamSize(),
                s.getTotalUsers(),
                s.getCustomerCount(),
                s.getMonthlyRevenue(),
                s.getRevenueGrowthRate(),
                s.getTotalFunding(),
                s.getMonthlyBurnRate(),
                s.getRunwayMonths(),
                s.getValuation(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getName(),
                owner == null ? null : owner.getEmail(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}