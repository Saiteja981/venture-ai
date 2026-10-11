package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;

import java.time.LocalDateTime;

public record AdminUserSummaryResponse(
        Long id,
        String name,
        String email,
        Role role,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static AdminUserSummaryResponse from(User u) {
        return new AdminUserSummaryResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.isEnabled() ? UserStatus.ACTIVE : UserStatus.DEACTIVATED,
                u.getCreatedAt()
        );
    }
}