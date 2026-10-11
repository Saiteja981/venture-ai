package com.ventureai.backend.service;

import com.ventureai.backend.dto.PagedResponse;
import com.ventureai.backend.dto.StartupDiscoveryResponse;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.StartupSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class StartupDiscoveryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 12;

    private final StartupRepository startupRepository;

    public StartupDiscoveryService(StartupRepository startupRepository) {
        this.startupRepository = startupRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<StartupDiscoveryResponse> discover(
            String search,
            String industry,
            String startupStage,
            String businessModel,
            BigDecimal minFunding,
            BigDecimal maxFunding,
            BigDecimal minRevenue,
            BigDecimal maxRevenue,
            Integer minTeamSize,
            Integer maxTeamSize,
            Long minUsers,
            Long maxUsers,
            String sort,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        if (size <= 0) safeSize = DEFAULT_PAGE_SIZE;
        int safePage = Math.max(page, 0);

        Specification<Startup> spec = Specification
                .where(StartupSpecifications.searchText(search))
                .and(StartupSpecifications.hasIndustry(industry))
                .and(StartupSpecifications.hasStage(startupStage))
                .and(StartupSpecifications.hasBusinessModel(businessModel))
                .and(StartupSpecifications.fundingBetween(minFunding, maxFunding))
                .and(StartupSpecifications.revenueBetween(minRevenue, maxRevenue))
                .and(StartupSpecifications.teamSizeBetween(minTeamSize, maxTeamSize))
                .and(StartupSpecifications.usersBetween(minUsers, maxUsers));

        Pageable pageable = PageRequest.of(safePage, safeSize, resolveSort(sort));

        Page<Startup> result = startupRepository.findAll(spec, pageable);

        Page<StartupDiscoveryResponse> mapped = result.map(StartupDiscoveryResponse::from);

        return PagedResponse.from(mapped);
    }

    /**
     * Whitelist of allowed sort options. Any unrecognized value falls back to "newest".
     */
    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.trim().toLowerCase()) {
            case "newest"          -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest"          -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "funding-high"    -> Sort.by(Sort.Direction.DESC, "totalFunding");
            case "funding-low"     -> Sort.by(Sort.Direction.ASC, "totalFunding");
            case "revenue-high"    -> Sort.by(Sort.Direction.DESC, "monthlyRevenue");
            case "revenue-low"     -> Sort.by(Sort.Direction.ASC, "monthlyRevenue");
            case "growth-high"     -> Sort.by(Sort.Direction.DESC, "revenueGrowthRate");
            case "valuation-high"  -> Sort.by(Sort.Direction.DESC, "valuation");
            case "team-high"       -> Sort.by(Sort.Direction.DESC, "teamSize");
            default                -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}