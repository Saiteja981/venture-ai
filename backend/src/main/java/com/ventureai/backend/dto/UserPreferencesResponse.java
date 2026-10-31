package com.ventureai.backend.dto;

import com.ventureai.backend.entity.User;

public record UserPreferencesResponse(
        boolean notifyCollab,
        boolean notifyMessages,
        boolean notifyAiEvents,
        boolean notifyUpdates,
        Integer recommendationsPerPage
) {
    public static UserPreferencesResponse from(User u) {
        return new UserPreferencesResponse(
                u.isNotifyCollab(),
                u.isNotifyMessages(),
                u.isNotifyAiEvents(),
                u.isNotifyUpdates(),
                u.getRecommendationsPerPage()
        );
    }
}