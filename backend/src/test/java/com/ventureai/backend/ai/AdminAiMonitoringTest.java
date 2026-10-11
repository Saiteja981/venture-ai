package com.ventureai.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.*;
import com.ventureai.backend.repository.*;
import com.ventureai.backend.service.AiExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAiMonitoringTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private StartupRepository startupRepository;
    @Autowired private InvestorRepository investorRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private CollaborationRequestRepository collaborationRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private SavedStartupRepository savedStartupRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private AiExecutionRepository executionRepository;
    @Autowired private AiFeedbackRepository feedbackRepository;
    @Autowired private AiExecutionService executionService;

    @BeforeEach
    void clean() {
        feedbackRepository.deleteAll();
        executionRepository.deleteAll();
        auditLogRepository.deleteAll();
        savedStartupRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        collaborationRepository.deleteAll();
        notificationRepository.deleteAll();
        startupRepository.deleteAll();
        investorRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String register(String name, String email, Role role) throws Exception {
        RegisterRequest req = new RegisterRequest(name, email, "Password123!", role);
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    @Test
    void adminCanAccessMonitoring() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);

        mvc.perform(get("/api/admin/ai/monitoring")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExecutions").value(0))
                .andExpect(jsonPath("$.note").exists());
    }

    @Test
    void monitoringAggregatesRealData() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("S", "s@test.com", Role.STARTUP);
        User s = user("s@test.com");

        // 3 successes + 1 failure for STARTUP_PREDICTION
        for (int i = 0; i < 3; i++) {
            executionService.record(s, AiTaskType.STARTUP_PREDICTION, "v1",
                    AiExecutionOutcome.SUCCESS, 100L + i, null);
        }
        executionService.record(s, AiTaskType.STARTUP_PREDICTION, null,
                AiExecutionOutcome.FAILED, 5000L, AiErrorCategory.TIMEOUT);

        // 1 success for RECOMMENDATION
        executionService.record(s, AiTaskType.RECOMMENDATION, "v1",
                AiExecutionOutcome.SUCCESS, 200L, null);

        mvc.perform(get("/api/admin/ai/monitoring")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExecutions").value(5))
                .andExpect(jsonPath("$.totalSuccessfulExecutions").value(4))
                .andExpect(jsonPath("$.totalFailedExecutions").value(1))
                .andExpect(jsonPath("$.byTask[?(@.task=='STARTUP_PREDICTION')].totalExecutions")
                        .value(4))
                .andExpect(jsonPath("$.byTask[?(@.task=='RECOMMENDATION')].totalExecutions")
                        .value(1));
    }

    @Test
    void monitoringReportsFeedbackStats() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        String s = register("S", "s@test.com", Role.STARTUP);
        User su = user("s@test.com");

        Long execId = executionService.record(su, AiTaskType.STARTUP_PREDICTION,
                "v1", AiExecutionOutcome.SUCCESS, 150L, null);

        mvc.perform(post("/api/ai/executions/" + execId + "/feedback")
                .header("Authorization", "Bearer " + s)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":4,\"category\":\"USEFUL\"}"));

        mvc.perform(get("/api/admin/ai/monitoring")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFeedbackSubmissions").value(1))
                .andExpect(jsonPath("$.averageFeedbackRating").value(4.0));
    }

    @Test
    void nonAdminForbidden() throws Exception {
        String startup = register("S", "s@test.com", Role.STARTUP);
        mvc.perform(get("/api/admin/ai/monitoring")
                        .header("Authorization", "Bearer " + startup))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/api/admin/ai/monitoring"))
                .andExpect(status().isUnauthorized());
    }
}