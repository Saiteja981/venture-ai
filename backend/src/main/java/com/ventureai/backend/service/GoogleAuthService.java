package com.ventureai.backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ventureai.backend.dto.AuthResponse;
import com.ventureai.backend.dto.GoogleAuthRequest;
import com.ventureai.backend.dto.GoogleAuthResponse;
import com.ventureai.backend.entity.AuthProvider;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.repository.UserRepository;
import com.ventureai.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService(UserRepository userRepository,
                             JwtService jwtService,
                             @Value("${google.oauth.client-id}") String clientId) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Exchange a Google ID token for either:
     *  - a full app JWT (existing Google user, or new user with role supplied), or
     *  - a "needs role" signal (new user, no role yet).
     *
     * Existing LOCAL accounts with the same email are rejected — the user must
     * first log in with their password, per the conservative account-linking
     * policy documented in Phase 27.
     */
    @Transactional
    public GoogleAuthResponse authenticate(GoogleAuthRequest request) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(request.idToken());
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid Google ID token");
        }
        if (idToken == null) {
            throw new BadCredentialsException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String subject = payload.getSubject();
        String email = payload.getEmail();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
        String name = (String) payload.get("name");

        if (!emailVerified) {
            throw new BadCredentialsException("Google account email is not verified");
        }
        if (subject == null || email == null) {
            throw new BadCredentialsException("Google token missing required claims");
        }

        // 1. Existing user by provider subject → log in
        User existingBySubject = userRepository.findByProviderSubject(subject).orElse(null);
        if (existingBySubject != null) {
            return GoogleAuthResponse.complete(buildAuthResponse(existingBySubject));
        }

        // 2. Existing user by email → reject if LOCAL; otherwise attach subject
        User existingByEmail = userRepository.findByEmail(email).orElse(null);
        if (existingByEmail != null) {
            if (existingByEmail.getAuthProvider() == AuthProvider.LOCAL) {
                throw new IllegalStateException(
                        "This email is already registered. Please sign in with your password.");
            }
            // Existing GOOGLE user with the same email but different subject
            // (rare) — treat as conflict.
            throw new IllegalStateException(
                    "This email is already registered with a different Google account.");
        }

        // 3. New user — need a role
        if (request.role() == null) {
            return GoogleAuthResponse.needsRole();
        }

        User created = User.builder()
                .name(name != null ? name : email)
                .email(email)
                .password("")   // no password; Google-only account
                .role(request.role())
                .authProvider(AuthProvider.GOOGLE)
                .providerSubject(subject)
                .enabled(true)
                .build();

        return GoogleAuthResponse.complete(buildAuthResponse(userRepository.save(created)));
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(
                user.getEmail(), user.getId(), user.getRole().name());
        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}