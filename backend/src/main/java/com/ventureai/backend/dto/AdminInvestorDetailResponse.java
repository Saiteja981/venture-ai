package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Investor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminInvestorDetailResponse(
        Long id,
        String investorName,
        String organizationName,
        String investorType,
        String preferredIndustries,
        String preferredStages,
        String preferredBusinessModels,
        BigDecimal minimumInvestment,
        BigDecimal maximumInvestment,
        String riskPreference,
        Integer investmentExperienceYears,
        Integer portfolioSize,
        String preferredLocation,
        String website,
        String bio,
        Long ownerId,
        String ownerName,
        String ownerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminInvestorDetailResponse from(Investor i) {
        var owner = i.getUser();
        return new AdminInvestorDetailResponse(
                i.getId(),
                i.getInvestorName(),
                i.getOrganizationName(),
                i.getInvestorType(),
                i.getPreferredIndustries(),
                i.getPreferredStages(),
                i.getPreferredBusinessModels(),
                i.getMinimumInvestment(),
                i.getMaximumInvestment(),
                i.getRiskPreference(),
                i.getInvestmentExperienceYears(),
                i.getPortfolioSize(),
                i.getPreferredLocation(),
                i.getWebsite(),
                i.getBio(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getName(),
                owner == null ? null : owner.getEmail(),
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }
}