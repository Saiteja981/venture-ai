package com.ventureai.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.*;
import com.ventureai.backend.repository.*;
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
class AuditLogTest {

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
    @Autowired private AiExecutionRepository aiExecutionRepository;
    @Autowired private AiFeedbackRepository aiFeedbackRepository;

    @BeforeEach
    void clean() {
        aiFeedbackRepository.deleteAll();
        aiExecutionRepository.deleteAll();
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

    /* ---------- Audit log creation ---------- */

    @Test
    void statusChangeCreatesAuditLog() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("Alice", "alice@test.com", Role.STARTUP);
        User alice = userRepository.findByEmail("alice@test.com").orElseThrow();

        mvc.perform(patch("/api/admin/users/" + alice.getId() + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEACTIVATED\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/audit-logs?action=USER_DEACTIVATE")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("USER_DEACTIVATE"))
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].targetId").value(alice.getId()));
    }

    @Test
    void selfDeactivationAttemptIsLoggedAsDenied() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        User adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();

        mvc.perform(patch("/api/admin/users/" + adminUser.getId() + "/status")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DEACTIVATED\"}"));

        mvc.perform(get("/api/admin/audit-logs?outcome=DENIED")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    /* ---------- Authorization ---------- */

    @Test
    void auditLogsReadableByAdminOnly() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        String startup = register("S", "s@test.com", Role.STARTUP);

        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + startup))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    /* ---------- Immutability ---------- */

    @Test
    void noAuditMutationEndpointsExist() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);

        // POST and DELETE on /api/admin/audit-logs must not be routed.
        // Both URLs match the GET mapping and therefore raise 405 (Method Not Allowed).
        mvc.perform(post("/api/admin/audit-logs").header("Authorization", "Bearer " + admin))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/admin/audit-logs").header("Authorization", "Bearer " + admin))
                .andExpect(status().isMethodNotAllowed());
    }
}