package com.ventureai.backend.dto;

import com.ventureai.backend.entity.AiFeedback;
import com.ventureai.backend.entity.FeedbackCategory;

import java.time.LocalDateTime;

public record AiFeedbackResponse(
        Long id,
        Long executionId,
        Integer rating,
        FeedbackCategory category,
        String comment,
        LocalDateTime createdAt
) {
    public static AiFeedbackResponse from(AiFeedback f) {
        return new AiFeedbackResponse(
                f.getId(),
                f.getExecution().getId(),
                f.getRating(),
                f.getCategory(),
                f.getComment(),
                f.getCreatedAt()
        );
    }
}