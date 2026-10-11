package com.ventureai.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Enumerated event types the frontend may report after a successful AI call.
 * The backend generates the standard title/message for each — this avoids
 * clients being able to create arbitrary notifications.
 */
public record AiEventRequest(@NotNull AiEventType eventType) {

    public enum AiEventType {
        AI_POTENTIAL_COMPLETED,
        PITCH_DECK_ANALYZED,
        RECOMMENDATIONS_LOADED
    }
}