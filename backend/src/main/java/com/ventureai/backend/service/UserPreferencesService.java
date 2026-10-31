package com.ventureai.backend.service;

import com.ventureai.backend.dto.UpdateUserPreferencesRequest;
import com.ventureai.backend.dto.UserPreferencesResponse;
import com.ventureai.backend.entity.NotificationType;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferencesService {

    private final UserRepository userRepository;

    public UserPreferencesService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse getForCurrentUser() {
        return UserPreferencesResponse.from(currentUser());
    }

    @Transactional
    public UserPreferencesResponse update(UpdateUserPreferencesRequest request) {
        User user = currentUser();

        user.setNotifyCollab(request.notifyCollab());
        user.setNotifyMessages(request.notifyMessages());
        user.setNotifyAiEvents(request.notifyAiEvents());
        user.setNotifyUpdates(request.notifyUpdates());

        // recommendationsPerPage is investor-only and optional.
        // Non-investor roles silently ignore the field.
        if (request.recommendationsPerPage() != null
                && user.getRole() == Role.INVESTOR) {
            // Enforce a small, sensible allow-list even though @Min/@Max
            // already bound the value.
            int v = request.recommendationsPerPage();
            if (v != 3 && v != 5 && v != 10 && v != 20) {
                throw new IllegalArgumentException(
                        "Recommendations per page must be one of 3, 5, 10, or 20");
            }
            user.setRecommendationsPerPage(v);
        }

        return UserPreferencesResponse.from(userRepository.save(user));
    }

    /**
     * Used by NotificationService to decide whether to persist a notification.
     * Maps each NotificationType to the corresponding preference.
     */
    @Transactional(readOnly = true)
    public boolean shouldDeliver(Long userId, NotificationType type) {
        return userRepository.findById(userId)
                .map(user -> switch (type) {
                    case COLLABORATION -> user.isNotifyCollab();
                    case MESSAGE -> user.isNotifyMessages();
                    case AI_RECOMMENDATION -> user.isNotifyAiEvents();
                    case SYSTEM, STARTUP_UPDATE, INVESTOR_UPDATE -> user.isNotifyUpdates();
                })
                .orElse(false);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}