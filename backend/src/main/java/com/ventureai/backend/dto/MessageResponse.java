package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        boolean read
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getContent(),
                m.getCreatedAt(),
                m.getReadAt() != null
        );
    }
}