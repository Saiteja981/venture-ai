package com.ventureai.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import com.ventureai.backend.entity.AuthProvider;
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'LOCAL'")
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * Google's "sub" claim — a stable, unique identifier per Google account.
     * Null for local accounts. Never expose this in responses.
     */
    @Column(name = "provider_subject", length = 128)
    private String providerSubject;

    @Column(name = "notify_collab", nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    @Builder.Default
    private boolean notifyCollab = true;

    @Column(name = "notify_messages", nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    @Builder.Default
    private boolean notifyMessages = true;

    @Column(name = "notify_ai_events", nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    @Builder.Default
    private boolean notifyAiEvents = true;

    @Column(name = "notify_updates", nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    @Builder.Default
    private boolean notifyUpdates = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "recommendations_per_page")
    private Integer recommendationsPerPage;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}