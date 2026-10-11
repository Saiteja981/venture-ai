package com.ventureai.backend.dto;

import com.ventureai.backend.entity.AuditLog;
import com.ventureai.backend.entity.AuditOutcome;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorEmail,
        String action,
        String targetType,
        Long targetId,
        AuditOutcome outcome,
        String details,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getActor() == null ? null : a.getActor().getId(),
                a.getActorEmail(),
                a.getAction(),
                a.getTargetType(),
                a.getTargetId(),
                a.getOutcome(),
                a.getDetails(),
                a.getCreatedAt()
        );
    }
}