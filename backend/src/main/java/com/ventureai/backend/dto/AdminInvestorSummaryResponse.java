package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Investor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminInvestorSummaryResponse(
        Long id,
        String investorName,
        String organizationName,
        String investorType,
        String preferredIndustries,
        String riskPreference,
        BigDecimal minimumInvestment,
        BigDecimal maximumInvestment,
        Integer portfolioSize,
        LocalDateTime createdAt
) {
    public static AdminInvestorSummaryResponse from(Investor i) {
        return new AdminInvestorSummaryResponse(
                i.getId(),
                i.getInvestorName(),
                i.getOrganizationName(),
                i.getInvestorType(),
                i.getPreferredIndustries(),
                i.getRiskPreference(),
                i.getMinimumInvestment(),
                i.getMaximumInvestment(),
                i.getPortfolioSize(),
                i.getCreatedAt()
        );
    }
}