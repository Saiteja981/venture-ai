package com.ventureai.backend.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long collaborationRequestId,
        Long startupId,
        String startupName,
        Long investorId,
        String investorName,
        String otherParticipantName,
        String lastMessagePreview,
        LocalDateTime lastMessageAt,
        long unreadCount
) {}