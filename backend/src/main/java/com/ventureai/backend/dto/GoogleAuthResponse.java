package com.ventureai.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response from the Google auth flow.
 *
 * - When `requiresRoleSelection` is true, the caller must POST again to
 *   /api/auth/google with a role field populated. No token is returned.
 * - Otherwise, `auth` contains the same payload as a normal login response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleAuthResponse(
        boolean requiresRoleSelection,
        AuthResponse auth
) {
    public static GoogleAuthResponse needsRole() {
        return new GoogleAuthResponse(true, null);
    }

    public static GoogleAuthResponse complete(AuthResponse auth) {
        return new GoogleAuthResponse(false, auth);
    }
}