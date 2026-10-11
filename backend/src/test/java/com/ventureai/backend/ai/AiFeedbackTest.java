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
class AiFeedbackTest {

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

    private Long seedExecutionFor(String email) {
        User u = userRepository.findByEmail(email).orElseThrow();
        return executionService.record(
                u, AiTaskType.STARTUP_PREDICTION, "v1",
                AiExecutionOutcome.SUCCESS, 200L, null);
    }

    @Test
    void ownerCanSubmitFeedback() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        Long execId = seedExecutionFor("a@test.com");

        mvc.perform(post("/api/ai/executions/" + execId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"category\":\"USEFUL\",\"comment\":\"Helpful\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.category").value("USEFUL"))
                .andExpect(jsonPath("$.executionId").value(execId));
    }

    @Test
    void duplicateFeedbackIsRejected() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        Long execId = seedExecutionFor("a@test.com");

        String body = "{\"rating\":4,\"category\":\"USEFUL\"}";
        mvc.perform(post("/api/ai/executions/" + execId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/ai/executions/" + execId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void nonOwnerCannotSubmitFeedback() throws Exception {
        String aToken = register("A", "a@test.com", Role.STARTUP);
        String bToken = register("B", "b@test.com", Role.STARTUP);

        Long aExecId = seedExecutionFor("a@test.com");

        mvc.perform(post("/api/ai/executions/" + aExecId + "/feedback")
                        .header("Authorization", "Bearer " + bToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"category\":\"USEFUL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingExecutionReturns404() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        mvc.perform(post("/api/ai/executions/999999/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"category\":\"USEFUL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRatingIsRejected() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        Long execId = seedExecutionFor("a@test.com");

        mvc.perform(post("/api/ai/executions/" + execId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":9,\"category\":\"USEFUL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commentTooLongIsRejected() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        Long execId = seedExecutionFor("a@test.com");
        String longComment = "x".repeat(501);

        mvc.perform(post("/api/ai/executions/" + execId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"category\":\"USEFUL\",\"comment\":\""
                                + longComment + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(post("/api/ai/executions/1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"category\":\"USEFUL\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanReadOwnFeedback() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        Long execId = seedExecutionFor("a@test.com");

        mvc.perform(post("/api/ai/executions/" + execId + "/feedback")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"category\":\"USEFUL\"}"));

        mvc.perform(get("/api/ai/executions/" + execId + "/feedback")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void readingFeedbackWithoutSubmissionReturns204() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        Long execId = seedExecutionFor("a@test.com");

        mvc.perform(get("/api/ai/executions/" + execId + "/feedback")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}