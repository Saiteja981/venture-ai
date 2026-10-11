package com.ventureai.backend.dto;

import com.ventureai.backend.entity.SavedStartup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SavedStartupResponse(
        Long id,
        LocalDateTime savedAt,
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
) {
    public static SavedStartupResponse from(SavedStartup s) {
        var st = s.getStartup();
        return new SavedStartupResponse(
                s.getId(),
                s.getCreatedAt(),
                st.getId(),
                st.getCompanyName(),
                st.getDescription(),
                st.getIndustry(),
                st.getStartupStage(),
                st.getBusinessModel(),
                st.getTeamSize(),
                st.getTotalUsers(),
                st.getCustomerCount(),
                st.getMonthlyRevenue(),
                st.getRevenueGrowthRate(),
                st.getTotalFunding(),
                st.getValuation(),
                st.getWebsite()
        );
    }
}