package com.ventureai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorResponse {

    private Long id;
    private String investorName;
    private String organizationName;
    private String investorType;
    private String preferredIndustries;
    private String preferredStages;
    private String preferredBusinessModels;
    private BigDecimal minimumInvestment;
    private BigDecimal maximumInvestment;
    private String riskPreference;
    private Integer investmentExperienceYears;
    private Integer portfolioSize;
    private String preferredLocation;
    private String website;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}