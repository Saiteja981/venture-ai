package com.ventureai.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvestorRequest {

    @NotBlank(message = "Investor name is required")
    @Size(max = 150, message = "Investor name must not exceed 150 characters")
    private String investorName;

    @Size(max = 150, message = "Organization name must not exceed 150 characters")
    private String organizationName;

    @NotBlank(message = "Investor type is required")
    @Size(max = 50, message = "Investor type must not exceed 50 characters")
    private String investorType;

    @NotBlank(message = "Preferred industries are required")
    @Size(max = 1000, message = "Preferred industries must not exceed 1000 characters")
    private String preferredIndustries;

    @NotBlank(message = "Preferred stages are required")
    @Size(max = 500, message = "Preferred stages must not exceed 500 characters")
    private String preferredStages;

    @Size(max = 500, message = "Preferred business models must not exceed 500 characters")
    private String preferredBusinessModels;

    @NotNull(message = "Minimum investment is required")
    @DecimalMin(value = "0.0", message = "Minimum investment must be zero or greater")
    private BigDecimal minimumInvestment;

    @NotNull(message = "Maximum investment is required")
    @DecimalMin(value = "0.0", message = "Maximum investment must be zero or greater")
    private BigDecimal maximumInvestment;

    @NotBlank(message = "Risk preference is required")
    @Size(max = 30, message = "Risk preference must not exceed 30 characters")
    private String riskPreference;

    @NotNull(message = "Investment experience years is required")
    @Min(value = 0, message = "Investment experience years must be zero or greater")
    private Integer investmentExperienceYears;

    @Min(value = 0, message = "Portfolio size must be zero or greater")
    private Integer portfolioSize;

    @Size(max = 150, message = "Preferred location must not exceed 150 characters")
    private String preferredLocation;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 5000, message = "Bio must not exceed 5000 characters")
    private String bio;
}