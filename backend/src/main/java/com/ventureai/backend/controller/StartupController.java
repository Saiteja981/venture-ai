package com.ventureai.backend.controller;

import com.ventureai.backend.dto.StartupRequest;
import com.ventureai.backend.dto.StartupResponse;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.service.StartupService;
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

@RestController
@RequestMapping("/api/startups")
public class StartupController {

    private final StartupService startupService;

    public StartupController(StartupService startupService) {
        this.startupService = startupService;
    }

    @PreAuthorize("hasRole('STARTUP')")
    @PostMapping
    public ResponseEntity<StartupResponse> createStartup(@Valid @RequestBody StartupRequest request) {
        String email = getAuthenticatedEmail();
        Startup startup = toEntity(request);
        Startup created = startupService.createStartup(startup, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PreAuthorize("hasRole('STARTUP')")
    @GetMapping("/my")
    public ResponseEntity<List<StartupResponse>> getMyStartups() {
        String email = getAuthenticatedEmail();
        List<StartupResponse> responses = startupService.getMyStartups(email)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('STARTUP')")
    @GetMapping("/{id}")
    public ResponseEntity<StartupResponse> getMyStartup(@PathVariable Long id) {
        String email = getAuthenticatedEmail();
        Startup startup = startupService.getMyStartup(id, email);
        return ResponseEntity.ok(toResponse(startup));
    }

    @PreAuthorize("hasRole('STARTUP')")
    @PutMapping("/{id}")
    public ResponseEntity<StartupResponse> updateMyStartup(@PathVariable Long id,
                                                           @Valid @RequestBody StartupRequest request) {
        String email = getAuthenticatedEmail();
        Startup updatedStartup = toEntity(request);
        Startup updated = startupService.updateMyStartup(id, updatedStartup, email);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PreAuthorize("hasRole('STARTUP')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMyStartup(@PathVariable Long id) {
        String email = getAuthenticatedEmail();
        startupService.deleteMyStartup(id, email);
        return ResponseEntity.noContent().build();
    }

    private String getAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private Startup toEntity(StartupRequest request) {
        return Startup.builder()
                .companyName(request.getCompanyName())
                .description(request.getDescription())
                .industry(request.getIndustry())
                .businessModel(request.getBusinessModel())
                .startupStage(request.getStartupStage())
                .foundedYear(request.getFoundedYear())
                .teamSize(request.getTeamSize())
                .totalUsers(request.getTotalUsers())
                .monthlyRevenue(request.getMonthlyRevenue())
                .revenueGrowthRate(request.getRevenueGrowthRate())
                .totalFunding(request.getTotalFunding())
                .monthlyBurnRate(request.getMonthlyBurnRate())
                .runwayMonths(request.getRunwayMonths())
                .valuation(request.getValuation())
                .customerCount(request.getCustomerCount())
                .website(request.getWebsite())
                .pitchDeckUrl(request.getPitchDeckUrl())
                .build();
    }

    private StartupResponse toResponse(Startup startup) {
        return StartupResponse.builder()
                .id(startup.getId())
                .companyName(startup.getCompanyName())
                .description(startup.getDescription())
                .industry(startup.getIndustry())
                .businessModel(startup.getBusinessModel())
                .startupStage(startup.getStartupStage())
                .foundedYear(startup.getFoundedYear())
                .teamSize(startup.getTeamSize())
                .totalUsers(startup.getTotalUsers())
                .monthlyRevenue(startup.getMonthlyRevenue())
                .revenueGrowthRate(startup.getRevenueGrowthRate())
                .totalFunding(startup.getTotalFunding())
                .monthlyBurnRate(startup.getMonthlyBurnRate())
                .runwayMonths(startup.getRunwayMonths())
                .valuation(startup.getValuation())
                .customerCount(startup.getCustomerCount())
                .website(startup.getWebsite())
                .pitchDeckUrl(startup.getPitchDeckUrl())
                .createdAt(startup.getCreatedAt())
                .updatedAt(startup.getUpdatedAt())
                .build();
    }
}