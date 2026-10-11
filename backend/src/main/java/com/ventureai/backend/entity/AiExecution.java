package com.ventureai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Privacy-conscious record of an AI service call.
 *
 * Explicitly does NOT store: request payloads, pitch-deck text, feature
 * vectors, tokens, or any user-supplied content. Only metadata about the
 * call itself.
 */
@Entity
@Table(name = "ai_executions", indexes = {
        @Index(name = "idx_ai_exec_user", columnList = "user_id"),
        @Index(name = "idx_ai_exec_task", columnList = "task"),
        @Index(name = "idx_ai_exec_outcome", columnList = "outcome"),
        @Index(name = "idx_ai_exec_created", columnList = "created_at"),
        @Index(name = "idx_ai_exec_correlation", columnList = "correlation_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
public class AiExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable: preserve the record even if the user is later deleted. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "task", nullable = false, length = 40)
    private AiTaskType task;

    @Column(name = "model_version", length = 80)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private AiExecutionOutcome outcome;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "correlation_id", nullable = false, length = 36, unique = true)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", length = 40)
    private AiErrorCategory errorCategory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.correlationId == null) {
            this.correlationId = UUID.randomUUID().toString();
        }
    }
}