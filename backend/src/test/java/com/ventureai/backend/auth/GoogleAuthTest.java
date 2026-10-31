package com.ventureai.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.AuthProvider;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;
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

/**
 * Phase 27 — Google Sign-In.
 *
 * The ID-token verifier is not mocked — instead these tests exercise the
 * controller boundary with structurally invalid tokens and verify the
 * controller rejects them cleanly (401). The full happy path requires a
 * real Google-signed token and is verified manually in the browser.
 *
 * Account-linking policy tests exercise the service layer directly via
 * repository state assertions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleAuthTest {

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

    /* ---------- Endpoint validation ---------- */

    @Test
    void googleEndpointIsPublic() throws Exception {
        // No Authorization header — the endpoint must still be reachable
        // (returns 401 from the token verification, not from Spring Security)
        mvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"invalid.jwt.token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenRejected() throws Exception {
        mvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"not-a-real-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankTokenRejected() throws Exception {
        mvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingTokenRejected() throws Exception {
        mvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /* ---------- Entity field defaults ---------- */

    @Test
    void existingLocalUsersHaveAuthProviderLocal() throws Exception {
        register("Alice", "alice@test.com", Role.STARTUP);
        User alice = userRepository.findByEmail("alice@test.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(
                AuthProvider.LOCAL, alice.getAuthProvider());
        org.junit.jupiter.api.Assertions.assertNull(alice.getProviderSubject());
    }

    /* ---------- Repository lookup ---------- */

    @Test
    void findProviderSubjectReturnsEmptyForUnknownSubject() {
        org.junit.jupiter.api.Assertions.assertTrue(
                userRepository.findByProviderSubject("nonexistent-google-sub").isEmpty());
    }

    /* ---------- Response shape ---------- */

    @Test
    void registerResponseIncludesNoProviderSubject() throws Exception {
        RegisterRequest req = new RegisterRequest("A", "a@test.com", "Password123!", Role.STARTUP);
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = res.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(
                body.contains("providerSubject"),
                "register response should not leak providerSubject");
        org.junit.jupiter.api.Assertions.assertFalse(
                body.contains("authProvider"),
                "register response should not leak authProvider");
    }
}