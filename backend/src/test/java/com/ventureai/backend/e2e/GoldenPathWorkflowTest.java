package com.ventureai.backend.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 25 — golden path end-to-end workflow.
 *
 * Walks the full product journey in one test:
 *   register both parties → create startup → investor profile → collaboration
 *   → accept → open conversation → exchange messages → save startup
 *   → verify portfolio → notifications → admin views → audit log → cleanup.
 *
 * Cleanup runs both before AND after each test (@BeforeEach + @AfterEach)
 * so this class never leaves FK-referencing rows behind for downstream
 * test classes. In particular, the audit log written at step 15 has an
 * FK to users and would otherwise block the next class's user cleanup.
 *
 * NOTE: Map.of() caps at 10 key-value pairs; larger payloads use
 * Map.ofEntries(Map.entry(...), ...).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoldenPathWorkflowTest {

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

    @AfterEach
    void tearDown() {
        clean();
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

    @Test
    void fullGoldenPath() throws Exception {
        /* 1. Register founder (STARTUP) */
        String founderToken = register("Narayana", "founder@test.com", Role.STARTUP);

        /* 2. Founder creates a startup profile */
        Map<String, Object> startupPayload = Map.ofEntries(
                Map.entry("companyName", "Test Startup"),
                Map.entry("description", "An E2E startup"),
                Map.entry("industry", "FinTech"),
                Map.entry("businessModel", "B2B SaaS"),
                Map.entry("startupStage", "PRE_SEED"),
                Map.entry("teamSize", 10),
                Map.entry("totalUsers", 1000),
                Map.entry("customerCount", 104),
                Map.entry("monthlyRevenue", new BigDecimal("15000")),
                Map.entry("revenueGrowthRate", new BigDecimal("12.5")),
                Map.entry("totalFunding", new BigDecimal("500000")),
                Map.entry("monthlyBurnRate", new BigDecimal("8000")),
                Map.entry("runwayMonths", new BigDecimal("18")),
                Map.entry("valuation", new BigDecimal("2000000"))
        );

        MvcResult startupRes = mvc.perform(post("/api/startups")
                        .header("Authorization", "Bearer " + founderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(startupPayload)))
                .andExpect(status().isCreated())
                .andReturn();
        Long startupId = json.readTree(startupRes.getResponse().getContentAsString())
                .get("id").asLong();

        /* 3. Register investor + create investor profile */
        String investorToken = register("Sushanth", "investor@test.com", Role.INVESTOR);

        Map<String, Object> investorPayload = Map.ofEntries(
                Map.entry("investorName", "Sushanth Capital"),
                Map.entry("organizationName", "Sushanth Ventures"),
                Map.entry("investorType", "VC_FIRM"),
                Map.entry("preferredIndustries", "FinTech,SaaS"),
                Map.entry("preferredStages", "PRE_SEED,SEED"),
                Map.entry("preferredBusinessModels", "B2B SaaS"),
                Map.entry("minimumInvestment", new BigDecimal("25000")),
                Map.entry("maximumInvestment", new BigDecimal("500000")),
                Map.entry("riskPreference", "MEDIUM"),
                Map.entry("investmentExperienceYears", 5),
                Map.entry("portfolioSize", 10),
                Map.entry("preferredLocation", "San Francisco")
        );

        mvc.perform(post("/api/investors")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(investorPayload)))
                .andExpect(status().isCreated());

        /* 4. Investor discovers the startup */
        mvc.perform(get("/api/startups/discover")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        /* 5. Investor sends collaboration request */
        MvcResult collabRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        Long collabId = json.readTree(collabRes.getResponse().getContentAsString())
                .get("id").asLong();

        /* 6. Founder accepts the pending request */
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                        .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk());

        /* 7. Investor opens a conversation */
        MvcResult convRes = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andReturn();
        Long convId = json.readTree(convRes.getResponse().getContentAsString())
                .get("id").asLong();

        /* 8. Send a message */
        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hi, looking forward to working together!\"}"))
                .andExpect(status().isOk());

        /* 9. Founder replies */
        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + founderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Thanks! Let's set up a call.\"}"))
                .andExpect(status().isOk());

        /* 10. Both see two messages */
        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        /* 11. Investor saves the startup */
        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/investors/me/saved-startups")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        /* 12. Portfolio includes the accepted collaboration */
        mvc.perform(get("/api/investors/me/portfolio")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCollaborations").value(1))
                .andExpect(jsonPath("$.startups.length()").value(1))
                .andExpect(jsonPath("$.startups[0].companyName").value("Test Startup"));

        /* 13. Notifications were created */
        long notifCount = notificationRepository.count();
        assertTrue(notifCount >= 3,
                "expected at least 3 notifications (request, accept, messages), got " + notifCount);

        /* 14. Admin sees consistent aggregates */
        String adminToken = register("Admin", "admin@test.com", Role.ADMIN);

        mvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.totalStartups").value(1))
                .andExpect(jsonPath("$.totalInvestors").value(1));

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mvc.perform(get("/api/admin/startups")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mvc.perform(get("/api/admin/investors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        /* 15. Admin deactivates the founder → audit log written */
        Long founderId = userRepository.findByEmail("founder@test.com").orElseThrow().getId();
        mvc.perform(patch("/api/admin/users/" + founderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DEACTIVATED\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/audit-logs?action=USER_DEACTIVATE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));

        /* 16. Investor removes bookmark */
        mvc.perform(delete("/api/investors/me/saved-startups/" + startupId)
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/investors/me/saved-startups")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(jsonPath("$.length()").value(0));
    }
}