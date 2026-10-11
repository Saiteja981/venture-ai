package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long id,
        String name,
        String email,
        Role role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminUserDetailResponse from(User u) {
        return new AdminUserDetailResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.isEnabled() ? UserStatus.ACTIVE : UserStatus.DEACTIVATED,
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}