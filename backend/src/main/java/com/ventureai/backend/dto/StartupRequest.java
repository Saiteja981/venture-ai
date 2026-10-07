package com.ventureai.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
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
public class StartupRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String companyName;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Industry is required")
    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;

    @NotBlank(message = "Business model is required")
    @Size(max = 100, message = "Business model must not exceed 100 characters")
    private String businessModel;

    @NotBlank(message = "Startup stage is required")
    @Size(max = 50, message = "Startup stage must not exceed 50 characters")
    private String startupStage;

    @Min(value = 1900, message = "Founded year must be at least 1900")
    @Max(value = 2100, message = "Founded year must not exceed 2100")
    private Integer foundedYear;

    @NotNull(message = "Team size is required")
    @Min(value = 1, message = "Team size must be at least 1")
    private Integer teamSize;

    @NotNull(message = "Total users is required")
    @Min(value = 0, message = "Total users must be zero or greater")
    private Long totalUsers;

    @NotNull(message = "Monthly revenue is required")
    @DecimalMin(value = "0.0", message = "Monthly revenue must be zero or greater")
    private BigDecimal monthlyRevenue;

    @NotNull(message = "Revenue growth rate is required")
    private BigDecimal revenueGrowthRate;

    @NotNull(message = "Total funding is required")
    @DecimalMin(value = "0.0", message = "Total funding must be zero or greater")
    private BigDecimal totalFunding;

    @NotNull(message = "Monthly burn rate is required")
    @DecimalMin(value = "0.0", message = "Monthly burn rate must be zero or greater")
    private BigDecimal monthlyBurnRate;

    @NotNull(message = "Runway months is required")
    @DecimalMin(value = "0.0", message = "Runway months must be zero or greater")
    private BigDecimal runwayMonths;

    @DecimalMin(value = "0.0", message = "Valuation must be zero or greater")
    private BigDecimal valuation;

    @NotNull(message = "Customer count is required")
    @Min(value = 0, message = "Customer count must be zero or greater")
    private Long customerCount;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

    @Size(max = 500, message = "Pitch deck URL must not exceed 500 characters")
    private String pitchDeckUrl;
}