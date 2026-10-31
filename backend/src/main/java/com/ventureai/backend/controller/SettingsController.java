package com.ventureai.backend.controller;

import com.ventureai.backend.dto.ChangePasswordRequest;
import com.ventureai.backend.dto.UpdateUserPreferencesRequest;
import com.ventureai.backend.dto.UserPreferencesResponse;
import com.ventureai.backend.service.PasswordChangeService;
import com.ventureai.backend.service.UserPreferencesService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User-level settings. All endpoints operate on the authenticated user
 * and ignore any client-supplied user ID.
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserPreferencesService preferencesService;
    private final PasswordChangeService passwordChangeService;

    public SettingsController(UserPreferencesService preferencesService,
                              PasswordChangeService passwordChangeService) {
        this.preferencesService = preferencesService;
        this.passwordChangeService = passwordChangeService;
    }

    @GetMapping("/preferences")
    public UserPreferencesResponse getPreferences() {
        return preferencesService.getForCurrentUser();
    }

    @PutMapping("/preferences")
    public UserPreferencesResponse updatePreferences(
            @Valid @RequestBody UpdateUserPreferencesRequest request) {
        return preferencesService.update(request);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordChangeService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}