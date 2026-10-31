package com.ventureai.backend.controller;

import com.ventureai.backend.dto.AuthResponse;
import com.ventureai.backend.dto.GoogleAuthRequest;
import com.ventureai.backend.dto.GoogleAuthResponse;
import com.ventureai.backend.dto.LoginRequest;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.service.AuthenticationService;
import com.ventureai.backend.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthenticationService authenticationService,
                          GoogleAuthService googleAuthService) {
        this.authenticationService = authenticationService;
        this.googleAuthService = googleAuthService;
    }

    /* ---------- Existing endpoints ---------- */

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    /* ---------- Phase 27: Google sign-in ---------- */

    @PostMapping("/google")
    public ResponseEntity<GoogleAuthResponse> googleAuth(
            @Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticate(request));
    }
}