package com.ventureai.backend.dto;

import com.ventureai.backend.entity.FeedbackCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitFeedbackRequest(
        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be between 1 and 5")
        @Max(value = 5, message = "rating must be between 1 and 5")
        Integer rating,

        @NotNull(message = "category is required")
        FeedbackCategory category,

        @Size(max = 500, message = "comment must be at most 500 characters")
        String comment
) {}