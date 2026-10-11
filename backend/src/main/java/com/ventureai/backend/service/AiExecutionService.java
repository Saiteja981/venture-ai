package com.ventureai.backend.service;

import com.ventureai.backend.entity.*;
import com.ventureai.backend.repository.AiExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AiExecutionService.class);

    private final AiExecutionRepository executionRepository;

    public AiExecutionService(AiExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    /**
     * Persist an AI execution record in its own transaction.
     *
     * If persistence fails, this method throws — the caller (AiProxyService)
     * wraps it in a try/catch so a monitoring write failure never breaks the
     * user-facing AI response.
     *
     * The record contains ONLY metadata about the call. No request payloads,
     * no pitch-deck text, no tokens.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long record(User user,
                       AiTaskType task,
                       String modelVersion,
                       AiExecutionOutcome outcome,
                       long latencyMs,
                       AiErrorCategory errorCategory) {
        AiExecution execution = AiExecution.builder()
                .user(user)
                .task(task)
                .modelVersion(modelVersion)
                .outcome(outcome)
                .latencyMs(latencyMs)
                .errorCategory(errorCategory)
                .build();
        AiExecution saved = executionRepository.save(execution);
        return saved.getId();
    }
}