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
class AdminUserManagementTest {

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

    private String adminToken() throws Exception {
        return register("Admin", "admin@test.com", Role.ADMIN);
    }

    /* ---------- Phase 21: User listing / search / filter ---------- */

    @Test
    void adminCanListUsers() throws Exception {
        String admin = adminToken();
        register("Alice", "alice@test.com", Role.STARTUP);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void adminCanSearchAndFilter() throws Exception {
        String admin = adminToken();
        register("Alice", "alice@test.com", Role.STARTUP);
        register("Bob", "bob@test.com", Role.INVESTOR);

        mvc.perform(get("/api/admin/users?search=alice").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("alice@test.com"));

        mvc.perform(get("/api/admin/users?role=INVESTOR").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void adminCanGetUserDetailSafely() throws Exception {
        String admin = adminToken();
        register("Alice", "alice@test.com", Role.STARTUP);
        User alice = userRepository.findByEmail("alice@test.com").orElseThrow();

        mvc.perform(get("/api/admin/users/" + alice.getId())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alice.getId()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    /* ---------- Phase 21: Status management ---------- */

    @Test
    void adminCanDeactivateAndReactivate() throws Exception {
        String admin = adminToken();
        register("Alice", "alice@test.com", Role.STARTUP);
        User alice = userRepository.findByEmail("alice@test.com").orElseThrow();

        mvc.perform(patch("/api/admin/users/" + alice.getId() + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEACTIVATED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@test.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(patch("/api/admin/users/" + alice.getId() + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotSelfDeactivate() throws Exception {
        String admin = adminToken();
        User adminUser = userRepository.findByEmail("admin@test.com").orElseThrow();

        mvc.perform(patch("/api/admin/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEACTIVATED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lastActiveAdminCannotBeDeactivated() throws Exception {
        register("Admin1", "a1@test.com", Role.ADMIN);
        String admin2 = register("Admin2", "a2@test.com", Role.ADMIN);
        User a1 = userRepository.findByEmail("a1@test.com").orElseThrow();

        mvc.perform(patch("/api/admin/users/" + a1.getId() + "/status")
                        .header("Authorization", "Bearer " + admin2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEACTIVATED\"}"))
                .andExpect(status().isOk());

        User a2 = userRepository.findByEmail("a2@test.com").orElseThrow();
        mvc.perform(patch("/api/admin/users/" + a2.getId() + "/status")
                        .header("Authorization", "Bearer " + admin2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEACTIVATED\"}"))
                .andExpect(status().isBadRequest());
    }

    /* ---------- Phase 21: Authorization ---------- */

    @Test
    void startupAndInvestorForbidden() throws Exception {
        String startup = register("S", "s@test.com", Role.STARTUP);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + startup))
                .andExpect(status().isForbidden());

        String investor = register("I", "i@test.com", Role.INVESTOR);
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + investor))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingUserReturns404() throws Exception {
        String admin = adminToken();
        mvc.perform(get("/api/admin/users/9999").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    /* ---------- Phase 24: Platform stats ---------- */

    @Test
    void platformStatsIncludesAiExecutionCount() throws Exception {
        String admin = adminToken();

        mvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.totalAiExecutions").value(0));
    }
}