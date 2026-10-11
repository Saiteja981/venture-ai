package com.ventureai.backend.saved;

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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SavedStartupTest {

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
    @Autowired private AiFeedbackRepository aiFeedbackRepository;
    @Autowired private AiExecutionRepository aiExecutionRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @BeforeEach
    void clean() {
        aiFeedbackRepository.deleteAll();
        aiExecutionRepository.deleteAll();
        auditLogRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        savedStartupRepository.deleteAll();
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

    private void createInvestorProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Investor inv = Investor.builder()
                .user(user).investorName("Inv " + email).organizationName("Org")
                .investorType("VC_FIRM").preferredIndustries("FinTech").preferredStages("SEED")
                .minimumInvestment(new BigDecimal("25000"))
                .maximumInvestment(new BigDecimal("500000"))
                .riskPreference("MEDIUM").investmentExperienceYears(5)
                .build();
        investorRepository.save(inv);
    }

    private Long createStartup(String ownerEmail, String company) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        Startup s = Startup.builder()
                .user(owner).companyName(company).description("Test")
                .industry("FinTech").businessModel("B2B SaaS").startupStage("SEED")
                .teamSize(5).totalUsers(1000L).customerCount(100L)
                .monthlyRevenue(new BigDecimal("10000"))
                .revenueGrowthRate(new BigDecimal("12.5"))
                .totalFunding(new BigDecimal("500000"))
                .monthlyBurnRate(new BigDecimal("5000"))
                .runwayMonths(new BigDecimal("18"))
                .valuation(new BigDecimal("2000000"))
                .build();
        return startupRepository.save(s).getId();
    }

    @Test
    void investorSavesStartup() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startupId").value(startupId))
                .andExpect(jsonPath("$.companyName").value("Alpha Co"));
    }

    @Test
    void duplicateSavePrevented() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                .header("Authorization", "Bearer " + investorToken));

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isConflict());
    }

    @Test
    void investorListsSavedStartups() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                .header("Authorization", "Bearer " + investorToken));

        mvc.perform(get("/api/investors/me/saved-startups")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].companyName").value("Alpha Co"));
    }

    @Test
    void investorRemovesSavedStartup() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                .header("Authorization", "Bearer " + investorToken));

        mvc.perform(delete("/api/investors/me/saved-startups/" + startupId)
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/investors/me/saved-startups")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void investorCannotSeeAnotherInvestorsSavedList() throws Exception {
        String inv1 = register("Inv1", "inv1@test.com", Role.INVESTOR);
        String inv2 = register("Inv2", "inv2@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv1@test.com");
        createInvestorProfile("inv2@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                .header("Authorization", "Bearer " + inv1));

        mvc.perform(get("/api/investors/me/saved-startups")
                        .header("Authorization", "Bearer " + inv2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void startupForbiddenFromSavedEndpoints() throws Exception {
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        mvc.perform(get("/api/investors/me/saved-startups")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/api/investors/me/saved-startups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingStartupReturns404() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        createInvestorProfile("inv@test.com");

        mvc.perform(post("/api/investors/me/saved-startups/9999")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void savedStatusReflectsState() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(get("/api/investors/me/saved-startups/" + startupId + "/status")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false));

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                .header("Authorization", "Bearer " + investorToken));

        mvc.perform(get("/api/investors/me/saved-startups/" + startupId + "/status")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(jsonPath("$.saved").value(true));
    }
}