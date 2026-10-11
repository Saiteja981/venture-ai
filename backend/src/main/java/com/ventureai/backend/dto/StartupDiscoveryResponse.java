package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Startup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Public-safe startup projection for investor discovery.
 * Deliberately excludes owner id / user id and any security-relevant fields.
 */
public record StartupDiscoveryResponse(
        Long id,
        String companyName,
        String description,
        String industry,
        String businessModel,
        String startupStage,
        Integer foundedYear,
        Integer teamSize,
        Long totalUsers,
        Long customerCount,
        BigDecimal monthlyRevenue,
        BigDecimal revenueGrowthRate,
        BigDecimal totalFunding,
        BigDecimal runwayMonths,
        BigDecimal valuation,
        String website,
        LocalDateTime createdAt
) {
    public static StartupDiscoveryResponse from(Startup s) {
        return new StartupDiscoveryResponse(
                s.getId(),
                s.getCompanyName(),
                s.getDescription(),
                s.getIndustry(),
                s.getBusinessModel(),
                s.getStartupStage(),
                s.getFoundedYear(),
                s.getTeamSize(),
                s.getTotalUsers(),
                s.getCustomerCount(),
                s.getMonthlyRevenue(),
                s.getRevenueGrowthRate(),
                s.getTotalFunding(),
                s.getRunwayMonths(),
                s.getValuation(),
                s.getWebsite(),
                s.getCreatedAt()
        );
    }
}