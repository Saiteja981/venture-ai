package com.ventureai.backend.dto;

import com.ventureai.backend.entity.Role;
import jakarta.validation.constraints.NotBlank;

/**
 * Sent by the frontend after the Google Sign-In popup succeeds.
 *
 * - `idToken` is the raw JWT string from `credentialResponse.credential`.
 * - `role` is optional; if null, the backend responds with a "role required"
 *   indicator and the frontend shows the role-picker screen.
 */
public record GoogleAuthRequest(
        @NotBlank(message = "idToken is required")
        String idToken,

        Role role
) {}