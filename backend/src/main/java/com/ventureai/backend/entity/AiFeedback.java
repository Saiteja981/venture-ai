package com.ventureai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User-submitted feedback on an AI execution.
 *
 * Policy: one feedback entry per (execution, user). Duplicate submissions
 * are rejected with 409. Feedback is subjective — never treated as ground
 * truth or as a measured-accuracy signal.
 */
@Entity
@Table(name = "ai_feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_feedback_execution_user",
                columnNames = {"execution_id", "user_id"}),
        indexes = {
                @Index(name = "idx_feedback_execution", columnList = "execution_id"),
                @Index(name = "idx_feedback_user", columnList = "user_id"),
                @Index(name = "idx_feedback_category", columnList = "category"),
                @Index(name = "idx_feedback_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"execution", "user"})
public class AiFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    private AiExecution execution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 1 (poor) .. 5 (excellent). */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private FeedbackCategory category;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}