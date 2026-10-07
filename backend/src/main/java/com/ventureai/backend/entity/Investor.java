package com.ventureai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "investors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_investors_user_id", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_investors_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "investor_name", nullable = false, length = 150)
    private String investorName;

    @Column(name = "organization_name", length = 150)
    private String organizationName;

    @Column(name = "investor_type", nullable = false, length = 50)
    private String investorType;

    @Column(name = "preferred_industries", nullable = false, length = 1000)
    private String preferredIndustries;

    @Column(name = "preferred_stages", nullable = false, length = 500)
    private String preferredStages;

    @Column(name = "preferred_business_models", length = 500)
    private String preferredBusinessModels;

    @Column(name = "minimum_investment", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumInvestment;

    @Column(name = "maximum_investment", nullable = false, precision = 15, scale = 2)
    private BigDecimal maximumInvestment;

    @Column(name = "risk_preference", nullable = false, length = 30)
    private String riskPreference;

    @Column(name = "investment_experience_years", nullable = false)
    private Integer investmentExperienceYears;

    @Column(name = "portfolio_size")
    private Integer portfolioSize;

    @Column(name = "preferred_location", length = 150)
    private String preferredLocation;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}