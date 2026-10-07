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
public class StartupResponse {

    private Long id;
    private String companyName;
    private String description;
    private String industry;
    private String businessModel;
    private String startupStage;
    private Integer foundedYear;
    private Integer teamSize;
    private Long totalUsers;
    private BigDecimal monthlyRevenue;
    private BigDecimal revenueGrowthRate;
    private BigDecimal totalFunding;
    private BigDecimal monthlyBurnRate;
    private BigDecimal runwayMonths;
    private BigDecimal valuation;
    private Long customerCount;
    private String website;
    private String pitchDeckUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}