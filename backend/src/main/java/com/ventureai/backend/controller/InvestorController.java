package com.ventureai.backend.controller;

import com.ventureai.backend.dto.InvestorRequest;
import com.ventureai.backend.dto.InvestorResponse;
import com.ventureai.backend.dto.PortfolioSummaryResponse;
import com.ventureai.backend.dto.SavedStartupResponse;
import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.service.InvestorService;
import com.ventureai.backend.service.PortfolioService;
import com.ventureai.backend.service.SavedStartupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investors")
public class InvestorController {

    private final InvestorService investorService;
    private final SavedStartupService savedStartupService;
    private final PortfolioService portfolioService;

    public InvestorController(InvestorService investorService,
                              SavedStartupService savedStartupService,
                              PortfolioService portfolioService) {
        this.investorService = investorService;
        this.savedStartupService = savedStartupService;
        this.portfolioService = portfolioService;
    }

    /* ---------- Existing: Investor profile CRUD ---------- */

    @PreAuthorize("hasRole('INVESTOR')")
    @PostMapping
    public ResponseEntity<InvestorResponse> createInvestor(@Valid @RequestBody InvestorRequest request) {
        String email = getAuthenticatedEmail();
        Investor investor = toEntity(request);
        Investor created = investorService.createInvestor(investor, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PreAuthorize("hasRole('INVESTOR')")
    @GetMapping("/me")
    public ResponseEntity<InvestorResponse> getMyInvestorProfile() {
        String email = getAuthenticatedEmail();
        Investor investor = investorService.getMyInvestorProfile(email);
        return ResponseEntity.ok(toResponse(investor));
    }

    @PreAuthorize("hasRole('INVESTOR')")
    @PutMapping("/me")
    public ResponseEntity<InvestorResponse> updateMyInvestorProfile(@Valid @RequestBody InvestorRequest request) {
        String email = getAuthenticatedEmail();
        Investor updatedInvestor = toEntity(request);
        Investor updated = investorService.updateMyInvestorProfile(updatedInvestor, email);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PreAuthorize("hasRole('INVESTOR')")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyInvestorProfile() {
        String email = getAuthenticatedEmail();
        investorService.deleteMyInvestorProfile(email);
        return ResponseEntity.noContent().build();
    }

    /* ---------- Phase 20: Saved Startups ---------- */

    @PostMapping("/me/saved-startups/{startupId}")
    @PreAuthorize("hasRole('INVESTOR')")
    public ResponseEntity<SavedStartupResponse> saveStartup(@PathVariable Long startupId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStartupService.save(startupId));
    }

    @GetMapping("/me/saved-startups")
    @PreAuthorize("hasRole('INVESTOR')")
    public List<SavedStartupResponse> listSaved() {
        return savedStartupService.list();
    }

    @DeleteMapping("/me/saved-startups/{startupId}")
    @PreAuthorize("hasRole('INVESTOR')")
    public ResponseEntity<Void> removeSaved(@PathVariable Long startupId) {
        savedStartupService.remove(startupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/saved-startups/{startupId}/status")
    @PreAuthorize("hasRole('INVESTOR')")
    public Map<String, Boolean> savedStatus(@PathVariable Long startupId) {
        return Map.of("saved", savedStartupService.isSaved(startupId));
    }

    /* ---------- Phase 20: Portfolio ---------- */

    @GetMapping("/me/portfolio")
    @PreAuthorize("hasRole('INVESTOR')")
    public PortfolioSummaryResponse portfolio() {
        return portfolioService.summary();
    }

    /* ---------- helpers ---------- */

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private Investor toEntity(InvestorRequest request) {
        return Investor.builder()
                .investorName(request.getInvestorName())
                .organizationName(request.getOrganizationName())
                .investorType(request.getInvestorType())
                .preferredIndustries(request.getPreferredIndustries())
                .preferredStages(request.getPreferredStages())
                .preferredBusinessModels(request.getPreferredBusinessModels())
                .minimumInvestment(request.getMinimumInvestment())
                .maximumInvestment(request.getMaximumInvestment())
                .riskPreference(request.getRiskPreference())
                .investmentExperienceYears(request.getInvestmentExperienceYears())
                .portfolioSize(request.getPortfolioSize())
                .preferredLocation(request.getPreferredLocation())
                .website(request.getWebsite())
                .bio(request.getBio())
                .build();
    }

    private InvestorResponse toResponse(Investor investor) {
        return InvestorResponse.builder()
                .id(investor.getId())
                .investorName(investor.getInvestorName())
                .organizationName(investor.getOrganizationName())
                .investorType(investor.getInvestorType())
                .preferredIndustries(investor.getPreferredIndustries())
                .preferredStages(investor.getPreferredStages())
                .preferredBusinessModels(investor.getPreferredBusinessModels())
                .minimumInvestment(investor.getMinimumInvestment())
                .maximumInvestment(investor.getMaximumInvestment())
                .riskPreference(investor.getRiskPreference())
                .investmentExperienceYears(investor.getInvestmentExperienceYears())
                .portfolioSize(investor.getPortfolioSize())
                .preferredLocation(investor.getPreferredLocation())
                .website(investor.getWebsite())
                .bio(investor.getBio())
                .createdAt(investor.getCreatedAt())
                .updatedAt(investor.getUpdatedAt())
                .build();
    }
}