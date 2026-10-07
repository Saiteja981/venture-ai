package com.ventureai.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class SecurityTestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Public endpoint is working";
    }

    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "Authenticated user endpoint is working";
    }

    @GetMapping("/startup")
    @PreAuthorize("hasRole('STARTUP')")
    public String startupEndpoint() {
        return "Startup role endpoint is working";
    }

    @GetMapping("/investor")
    @PreAuthorize("hasRole('INVESTOR')")
    public String investorEndpoint() {
        return "Investor role endpoint is working";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "Admin role endpoint is working";
    }
}