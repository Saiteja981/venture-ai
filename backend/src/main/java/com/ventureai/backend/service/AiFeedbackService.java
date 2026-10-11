package com.ventureai.backend.service;

import com.ventureai.backend.dto.AiFeedbackResponse;
import com.ventureai.backend.dto.SubmitFeedbackRequest;
import com.ventureai.backend.entity.AiExecution;
import com.ventureai.backend.entity.AiFeedback;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.AiFeedbackRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiFeedbackService {

    private final AiFeedbackRepository feedbackRepository;
    private final AiExecutionRepository executionRepository;
    private final UserRepository userRepository;

    public AiFeedbackService(AiFeedbackRepository feedbackRepository,
                             AiExecutionRepository executionRepository,
                             UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.executionRepository = executionRepository;
        this.userRepository = userRepository;
    }

    /** Submit feedback on one of the caller's own executions. */
    @Transactional
    public AiFeedbackResponse submit(Long executionId, SubmitFeedbackRequest request) {
        User user = currentUser();
        AiExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        // Ownership: user must own the execution
        if (execution.getUser() == null
                || !execution.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot submit feedback for this execution");
        }

        // Duplicate policy: at most one feedback entry per (execution, user).
        if (feedbackRepository.existsByExecutionIdAndUserId(executionId, user.getId())) {
            throw new IllegalArgumentException(
                    "Feedback already exists for this execution");
        }

        AiFeedback feedback = AiFeedback.builder()
                .execution(execution)
                .user(user)
                .rating(request.rating())
                .category(request.category())
                .comment(trimToNull(request.comment()))
                .build();

        try {
            feedback = feedbackRepository.save(feedback);
        } catch (DataIntegrityViolationException race) {
            // Concurrent submission lost the race against the unique constraint
            throw new IllegalArgumentException(
                    "Feedback already exists for this execution");
        }

        return AiFeedbackResponse.from(feedback);
    }

    /**
     * Returns the caller's own feedback for an execution, or null when none
     * exists yet. Ownership is enforced even on read.
     */
    @Transactional(readOnly = true)
    public AiFeedbackResponse getMine(Long executionId) {
        User user = currentUser();
        AiExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        if (execution.getUser() == null
                || !execution.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot view feedback for this execution");
        }

        return feedbackRepository
                .findByExecutionIdAndUserId(executionId, user.getId())
                .map(AiFeedbackResponse::from)
                .orElse(null);
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}