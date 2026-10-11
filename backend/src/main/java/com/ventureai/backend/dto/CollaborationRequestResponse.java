package com.ventureai.backend.dto;

import com.ventureai.backend.entity.CollaborationRequest;
import com.ventureai.backend.entity.CollaborationStatus;

import java.time.LocalDateTime;

public record CollaborationRequestResponse(
        Long id,
        Long investorId,
        String investorName,
        String investorOrganization,
        String investorType,
        Long startupId,
        String startupName,
        String startupIndustry,
        String startupStage,
        String message,
        CollaborationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CollaborationRequestResponse from(CollaborationRequest r) {
        return new CollaborationRequestResponse(
                r.getId(),
                r.getInvestor().getId(),
                r.getInvestor().getInvestorName(),
                r.getInvestor().getOrganizationName(),
                r.getInvestor().getInvestorType(),
                r.getStartup().getId(),
                r.getStartup().getCompanyName(),
                r.getStartup().getIndustry(),
                r.getStartup().getStartupStage(),
                r.getMessage(),
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}