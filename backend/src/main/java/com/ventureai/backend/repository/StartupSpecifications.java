package com.ventureai.backend.repository;

import com.ventureai.backend.entity.Startup;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Type-safe JPA Specifications for startup discovery.
 * All methods return a "no-op" (conjunction) when the parameter is null/blank,
 * so they can be composed without null checks at the call site.
 */
public final class StartupSpecifications {

    private StartupSpecifications() {}

    public static Specification<Startup> searchText(String search) {
        if (search == null || search.isBlank()) return null;
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("companyName")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("industry")), pattern)
        );
    }

    public static Specification<Startup> hasIndustry(String industry) {
        if (industry == null || industry.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("industry")), industry.trim().toLowerCase());
    }

    public static Specification<Startup> hasStage(String stage) {
        if (stage == null || stage.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("startupStage")), stage.trim().toLowerCase());
    }

    public static Specification<Startup> hasBusinessModel(String model) {
        if (model == null || model.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("businessModel")), model.trim().toLowerCase());
    }

    public static Specification<Startup> fundingBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            if (min != null && max != null)
                return cb.between(root.get("totalFunding"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("totalFunding"), min);
            return cb.lessThanOrEqualTo(root.get("totalFunding"), max);
        };
    }

    public static Specification<Startup> revenueBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            if (min != null && max != null)
                return cb.between(root.get("monthlyRevenue"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("monthlyRevenue"), min);
            return cb.lessThanOrEqualTo(root.get("monthlyRevenue"), max);
        };
    }

    public static Specification<Startup> teamSizeBetween(Integer min, Integer max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            if (min != null && max != null)
                return cb.between(root.get("teamSize"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("teamSize"), min);
            return cb.lessThanOrEqualTo(root.get("teamSize"), max);
        };
    }

    public static Specification<Startup> usersBetween(Long min, Long max) {
        if (min == null && max == null) return null;
        return (root, query, cb) -> {
            if (min != null && max != null)
                return cb.between(root.get("totalUsers"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("totalUsers"), min);
            return cb.lessThanOrEqualTo(root.get("totalUsers"), max);
        };
    }
}