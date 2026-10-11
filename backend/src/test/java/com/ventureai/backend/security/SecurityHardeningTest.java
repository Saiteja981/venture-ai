package com.ventureai.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.Role;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 25 — security hardening.
 *
 * Focused on gaps NOT covered by the existing SecurityTest, MessagingTest,
 * AiFeedbackTest, SavedStartupTest, and CollaborationTest suites:
 *   - sensitive data leaks in response bodies
 *   - stack-trace leaks in error responses
 *   - JWT signature tampering (as opposed to structural malformation)
 *   - broad malformed-token sweep
 *   - admin user detail credential stripping
 *
 * Deliberately does NOT repeat role-boundary or cross-resource IDOR tests
 * that already exist elsewhere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningTest {

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

    /* =========================================================
     * 1. Sensitive data never leaks in auth responses
     * ========================================================= */

    @Test
    void registerResponseNeverExposesPasswordOrHash() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Leak Test", "leak@test.com", "Password123!", Role.STARTUP);
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        assertFalse(body.toLowerCase().contains("\"password\""),
                "register response leaked password field");
        assertFalse(body.contains("$2a$"), "register response leaked BCrypt hash ($2a$)");
        assertFalse(body.contains("$2b$"), "register response leaked BCrypt hash ($2b$)");
        assertFalse(body.contains("$2y$"), "register response leaked BCrypt hash ($2y$)");
    }

    @Test
    void loginResponseNeverExposesPasswordOrHash() throws Exception {
        register("Leak Test", "leak@test.com", Role.STARTUP);

        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"leak@test.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        assertFalse(body.toLowerCase().contains("\"password\""),
                "login response leaked password field");
        assertFalse(body.contains("$2a$"), "login response leaked BCrypt hash");
        assertFalse(body.contains("$2b$"), "login response leaked BCrypt hash");
    }

    /* =========================================================
     * 2. Admin user detail strips credentials
     * ========================================================= */

    @Test
    void adminUserDetailNeverExposesCredentials() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("Target", "target@test.com", Role.STARTUP);
        Long targetId = userRepository.findByEmail("target@test.com").orElseThrow().getId();

        MvcResult res = mvc.perform(get("/api/admin/users/" + targetId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        assertFalse(body.toLowerCase().contains("\"password\""),
                "admin user detail leaked password field");
        assertFalse(body.contains("$2a$"), "admin user detail leaked BCrypt hash");
        assertFalse(body.contains("$2b$"), "admin user detail leaked BCrypt hash");
    }

    /* =========================================================
     * 3. Stack traces never leak in error responses
     * ========================================================= */

    @Test
    void notFoundErrorDoesNotLeakStackTrace() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);

        MvcResult res = mvc.perform(get("/api/admin/users/9999999")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        assertFalse(body.contains("at com.ventureai"),
                "error response leaked full stack trace");
        assertFalse(body.contains("at java."),
                "error response leaked JDK stack trace");
        assertFalse(body.contains("java.lang.NullPointerException"),
                "error response leaked exception class");
        assertFalse(body.contains("org.springframework"),
                "error response leaked Spring internals");
    }

    /* =========================================================
     * 4. JWT signature tampering rejected (distinct from malformed)
     * ========================================================= */

    @Test
    void signatureTamperedJwtRejected() throws Exception {
        String token = register("Admin", "admin@test.com", Role.ADMIN);

        // Flip a MIDDLE character of the base64url signature.
        //
        // Flipping the LAST character is unreliable: a 32-byte HMAC-SHA256
        // signature encodes to 43 base64url characters, and the final
        // character only carries 2 significant bits. Mutating the padding
        // bits leaves the decoded signature bytes unchanged, so the token
        // remains valid. Flipping a middle character always changes at
        // least one decoded byte.
        int sigStart = token.lastIndexOf('.') + 1;
        int mid = sigStart + (token.length() - sigStart) / 2;
        char original = token.charAt(mid);
        char flipped = (original == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, mid) + flipped + token.substring(mid + 1);

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void payloadTamperedJwtRejected() throws Exception {
        String token = register("Admin", "admin@test.com", Role.ADMIN);

        // Swap the middle segment (payload) with a different but valid base64
        String[] parts = token.split("\\.");
        assert parts.length == 3;
        String tampered = parts[0] + ".ZmFrZXBheWxvYWQ." + parts[2];

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    /* =========================================================
     * 5. Broad malformed-token sweep
     * ========================================================= */

    @Test
    void allMalformedTokenVariantsRejected() throws Exception {
        String[] badHeaders = {
                "Bearer ",                               // empty
                "Bearer abc",                            // single token
                "Bearer a.b.c",                          // structurally valid, nonsense content
                "Bearer eyJhbGciOiJIUzI1NiJ9",           // header only
                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.",  // missing signature
                "Bearer AAAA.AAAA.AAAA",                 // all A's
                "Bearer " + "Z".repeat(1000),            // extremely long
                "Basic dXNlcjpwYXNz",                    // wrong scheme
                "Token abc",                             // wrong scheme prefix
                "abc",                                   // no scheme at all
        };

        for (String header : badHeaders) {
            mvc.perform(get("/api/admin/users").header("Authorization", header))
                    .andExpect(status().isUnauthorized());
        }
    }
}