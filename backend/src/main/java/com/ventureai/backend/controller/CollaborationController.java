package com.ventureai.backend.controller;

import com.ventureai.backend.dto.CollaborationRequestCreateRequest;
import com.ventureai.backend.dto.CollaborationRequestResponse;
import com.ventureai.backend.service.CollaborationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collaborations")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INVESTOR')")
    public ResponseEntity<CollaborationRequestResponse> create(
            @Valid @RequestBody CollaborationRequestCreateRequest request) {
        CollaborationRequestResponse created =
                collaborationService.createRequest(request.startupId(), request.message());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/investor")
    @PreAuthorize("hasRole('INVESTOR')")
    public List<CollaborationRequestResponse> listInvestorRequests() {
        return collaborationService.getInvestorRequests();
    }

    @GetMapping("/startup")
    @PreAuthorize("hasRole('STARTUP')")
    public List<CollaborationRequestResponse> listStartupRequests() {
        return collaborationService.getStartupRequests();
    }

    @GetMapping("/{id}")
    public CollaborationRequestResponse getById(@PathVariable Long id) {
        return collaborationService.getRequestById(id);
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('STARTUP')")
    public CollaborationRequestResponse accept(@PathVariable Long id) {
        return collaborationService.acceptRequest(id);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('STARTUP')")
    public CollaborationRequestResponse reject(@PathVariable Long id) {
        return collaborationService.rejectRequest(id);
    }

    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('INVESTOR')")
    public CollaborationRequestResponse withdraw(@PathVariable Long id) {
        return collaborationService.withdrawRequest(id);
    }
}