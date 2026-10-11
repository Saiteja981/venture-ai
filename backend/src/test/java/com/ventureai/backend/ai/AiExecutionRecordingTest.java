package com.ventureai.backend.ai;

import com.ventureai.backend.entity.*;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.AiFeedbackRepository;
import com.ventureai.backend.repository.UserRepository;
import com.ventureai.backend.service.AiExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AiExecutionRecordingTest {

    @Autowired private AiExecutionService executionService;
    @Autowired private AiExecutionRepository executionRepository;
    @Autowired private AiFeedbackRepository feedbackRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void clean() {
        feedbackRepository.deleteAll();
        executionRepository.deleteAll();
    }

    private User makeUser(String email) {
        return userRepository.save(User.builder()
                .name("User " + email)
                .email(email)
                .password("x")
                .role(Role.STARTUP)
                .build());
    }

    @Test
    void successfulExecutionIsRecorded() {
        User u = makeUser("a@test.com");

        Long id = executionService.record(
                u, AiTaskType.STARTUP_PREDICTION, "startup_potential_risk_v1",
                AiExecutionOutcome.SUCCESS, 250L, null);

        assertNotNull(id);
        AiExecution saved = executionRepository.findById(id).orElseThrow();
        assertEquals(AiTaskType.STARTUP_PREDICTION, saved.getTask());
        assertEquals("startup_potential_risk_v1", saved.getModelVersion());
        assertEquals(AiExecutionOutcome.SUCCESS, saved.getOutcome());
        assertEquals(250L, saved.getLatencyMs());
        assertNull(saved.getErrorCategory());
        assertEquals(u.getId(), saved.getUser().getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getCorrelationId());
    }

    @Test
    void failedExecutionIsRecordedWithErrorCategory() {
        User u = makeUser("b@test.com");

        Long id = executionService.record(
                u, AiTaskType.RECOMMENDATION, null,
                AiExecutionOutcome.FAILED, 5000L, AiErrorCategory.TIMEOUT);

        AiExecution saved = executionRepository.findById(id).orElseThrow();
        assertEquals(AiExecutionOutcome.FAILED, saved.getOutcome());
        assertEquals(AiErrorCategory.TIMEOUT, saved.getErrorCategory());
        assertNull(saved.getModelVersion());
    }

    @Test
    void executionWithNullUserIsRecorded() {
        Long id = executionService.record(
                null, AiTaskType.PITCH_DECK_ANALYSIS, "pitch_analyzer",
                AiExecutionOutcome.SUCCESS, 800L, null);

        AiExecution saved = executionRepository.findById(id).orElseThrow();
        assertNull(saved.getUser());
    }

    @Test
    void correlationIdsAreUnique() {
        User u = makeUser("c@test.com");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            Long id = executionService.record(
                    u, AiTaskType.STARTUP_PREDICTION, "v1",
                    AiExecutionOutcome.SUCCESS, 100L, null);
            String cid = executionRepository.findById(id).orElseThrow().getCorrelationId();
            assertTrue(ids.add(cid), "duplicate correlationId: " + cid);
        }
        assertEquals(10, ids.size());
    }

    @Test
    void latencyIsPersistedExactly() {
        User u = makeUser("d@test.com");
        Long id = executionService.record(
                u, AiTaskType.STARTUP_PREDICTION, "v1",
                AiExecutionOutcome.SUCCESS, 1234L, null);
        assertEquals(1234L,
                executionRepository.findById(id).orElseThrow().getLatencyMs());
    }
}