package com.ventureai.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CollaborationRequestCreateRequest(
        @NotNull(message = "startupId is required")
        Long startupId,

        @Size(max = 1000, message = "Message must be at most 1000 characters")
        String message
) {}