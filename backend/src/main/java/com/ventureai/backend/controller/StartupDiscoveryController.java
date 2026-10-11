package com.ventureai.backend.controller;

import com.ventureai.backend.dto.PagedResponse;
import com.ventureai.backend.dto.StartupDiscoveryResponse;
import com.ventureai.backend.service.StartupDiscoveryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Investor-facing startup discovery endpoint.
 * Access restricted to INVESTOR and ADMIN roles.
 * STARTUP users get 403 via Spring Security.
 */
@RestController
@RequestMapping("/api/startups/discover")
public class StartupDiscoveryController {

    private final StartupDiscoveryService service;

    public StartupDiscoveryController(StartupDiscoveryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('INVESTOR','ADMIN')")
    public PagedResponse<StartupDiscoveryResponse> discover(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String startupStage,
            @RequestParam(required = false) String businessModel,
            @RequestParam(required = false) BigDecimal minFunding,
            @RequestParam(required = false) BigDecimal maxFunding,
            @RequestParam(required = false) BigDecimal minRevenue,
            @RequestParam(required = false) BigDecimal maxRevenue,
            @RequestParam(required = false) Integer minTeamSize,
            @RequestParam(required = false) Integer maxTeamSize,
            @RequestParam(required = false) Long minUsers,
            @RequestParam(required = false) Long maxUsers,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return service.discover(
                search, industry, startupStage, businessModel,
                minFunding, maxFunding, minRevenue, maxRevenue,
                minTeamSize, maxTeamSize, minUsers, maxUsers,
                sort, page, size
        );
    }
}