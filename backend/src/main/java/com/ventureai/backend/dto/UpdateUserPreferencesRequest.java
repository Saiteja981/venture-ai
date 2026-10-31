package com.ventureai.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateUserPreferencesRequest(
        @NotNull Boolean notifyCollab,
        @NotNull Boolean notifyMessages,
        @NotNull Boolean notifyAiEvents,
        @NotNull Boolean notifyUpdates,
        @Min(value = 1, message = "recommendationsPerPage must be at least 1")
        @Max(value = 50, message = "recommendationsPerPage must be at most 50")
        Integer recommendationsPerPage
) {}