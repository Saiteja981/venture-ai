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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "startups",
        indexes = {
                @Index(name = "idx_startups_user_id", columnList = "user_id"),
                @Index(name = "idx_startups_industry", columnList = "industry"),
                @Index(name = "idx_startups_startup_stage", columnList = "startup_stage")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Startup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "industry", nullable = false, length = 100)
    private String industry;

    @Column(name = "business_model", nullable = false, length = 100)
    private String businessModel;

    @Column(name = "startup_stage", nullable = false, length = 50)
    private String startupStage;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "team_size", nullable = false)
    private Integer teamSize;

    @Column(name = "total_users", nullable = false)
    private Long totalUsers;

    @Column(name = "monthly_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyRevenue;

    @Column(name = "revenue_growth_rate", nullable = false, precision = 7, scale = 2)
    private BigDecimal revenueGrowthRate;

    @Column(name = "total_funding", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalFunding;

    @Column(name = "monthly_burn_rate", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyBurnRate;

    @Column(name = "runway_months", nullable = false, precision = 6, scale = 2)
    private BigDecimal runwayMonths;

    @Column(name = "valuation", precision = 18, scale = 2)
    private BigDecimal valuation;

    @Column(name = "customer_count", nullable = false)
    private Long customerCount;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "pitch_deck_url", length = 500)
    private String pitchDeckUrl;

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