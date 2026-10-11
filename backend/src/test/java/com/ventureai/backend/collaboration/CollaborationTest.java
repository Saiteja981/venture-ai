package com.ventureai.backend.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.repository.CollaborationRequestRepository;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.NotificationRepository;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.UserRepository;
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
import com.ventureai.backend.repository.ConversationRepository;
import com.ventureai.backend.repository.MessageRepository;
import com.ventureai.backend.repository.SavedStartupRepository;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.AiFeedbackRepository;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollaborationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private StartupRepository startupRepository;
    @Autowired private InvestorRepository investorRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private SavedStartupRepository savedStartupRepository;
    @Autowired private CollaborationRequestRepository collaborationRepository;
    @Autowired private AiFeedbackRepository aiFeedbackRepository;
    @Autowired private AiExecutionRepository aiExecutionRepository;

    @BeforeEach
    void clean() {
        aiFeedbackRepository.deleteAll();
        aiExecutionRepository.deleteAll();
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

    private Long createStartup(String ownerEmail, String companyName) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        Startup s = Startup.builder()
                .user(owner)
                .companyName(companyName)
                .description("Test startup")
                .industry("FinTech")
                .businessModel("B2B SaaS")
                .startupStage("SEED")
                .teamSize(5)
                .totalUsers(1000L)
                .customerCount(100L)
                .monthlyRevenue(new BigDecimal("10000"))
                .revenueGrowthRate(new BigDecimal("12.5"))
                .totalFunding(new BigDecimal("500000"))
                .monthlyBurnRate(new BigDecimal("5000"))
                .runwayMonths(new BigDecimal("18"))
                .valuation(new BigDecimal("2000000"))
                .build();
        return startupRepository.save(s).getId();
    }

    private Long createInvestorProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Investor inv = Investor.builder()
                .user(user)
                .investorName("Investor " + email)
                .organizationName("Org")
                .investorType("VC_FIRM")
                .preferredIndustries("FinTech")
                .preferredStages("SEED")
                .minimumInvestment(new BigDecimal("25000"))
                .maximumInvestment(new BigDecimal("500000"))
                .riskPreference("MEDIUM")
                .investmentExperienceYears(5)
                .build();
        return investorRepository.save(inv).getId();
    }

    /* 1. Investor creates request */
    @Test
    void investorCreatesRequest() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + ",\"message\":\"Interested\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startupName").value("Alpha Co"));
    }

    /* 2. Startup receives request */
    @Test
    void startupSeesIncomingRequest() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/collaborations")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startupId\":" + startupId + "}"));

        mvc.perform(get("/api/collaborations/startup")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    /* 3. Duplicate pending rejected — expects 409 Conflict */
    @Test
    void duplicatePendingRejected() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/collaborations")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startupId\":" + startupId + "}"));

        mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andExpect(status().isConflict());
    }

    /* 4. Investor sees only own requests */
    @Test
    void investorSeesOnlyOwnRequests() throws Exception {
        String inv1 = register("Inv1", "inv1@test.com", Role.INVESTOR);
        String inv2 = register("Inv2", "inv2@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv1@test.com");
        createInvestorProfile("inv2@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/collaborations")
                .header("Authorization", "Bearer " + inv1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startupId\":" + startupId + "}"));

        mvc.perform(get("/api/collaborations/investor")
                        .header("Authorization", "Bearer " + inv2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /* 5. Startup sees only owned startups */
    @Test
    void startupSeesOnlyOwnedStartupRequests() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String aliceToken = register("Alice", "alice@test.com", Role.STARTUP);
        register("Bob", "bob@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long aliceStartupId = createStartup("alice@test.com", "Alpha Co");
        createStartup("bob@test.com", "Beta Co");

        mvc.perform(post("/api/collaborations")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startupId\":" + aliceStartupId + "}"));

        mvc.perform(get("/api/collaborations/startup")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /* 6. Accept */
    @Test
    void startupAcceptsRequest() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    /* 7. Reject */
    @Test
    void startupRejectsRequest() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/reject")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    /* 8. Withdraw */
    @Test
    void investorWithdrawsPending() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/withdraw")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    /* 9. Investor cannot accept */
    @Test
    void investorCannotAcceptRequest() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isForbidden());
    }

    /* 10. Startup cannot create request */
    @Test
    void startupCannotCreateInvestorRequest() throws Exception {
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);

        mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + startupToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":1}"))
                .andExpect(status().isForbidden());
    }

    /* 11. Wrong owner cannot accept */
    @Test
    void wrongOwnerCannotAccept() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        String bobToken = register("Bob", "bob@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long aliceStartupId = createStartup("alice@test.com", "Alpha Co");
        createStartup("bob@test.com", "Beta Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + aliceStartupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    /* 12. Wrong investor cannot withdraw */
    @Test
    void wrongInvestorCannotWithdraw() throws Exception {
        String inv1 = register("Inv1", "inv1@test.com", Role.INVESTOR);
        String inv2 = register("Inv2", "inv2@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv1@test.com");
        createInvestorProfile("inv2@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + inv1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/withdraw")
                        .header("Authorization", "Bearer " + inv2))
                .andExpect(status().isForbidden());
    }

    /* 13. Unauthenticated */
    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(post("/api/collaborations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/collaborations/investor"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/collaborations/startup"))
                .andExpect(status().isUnauthorized());
    }

    /* 14. Missing -> 404 */
    @Test
    void missingRequestReturns404() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        createInvestorProfile("inv@test.com");

        mvc.perform(get("/api/collaborations/9999")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isNotFound());
    }

    /* 15. Notification on new request */
    @Test
    void notificationCreatedOnNewRequest() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        mvc.perform(post("/api/collaborations")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startupId\":" + startupId + "}"));

        User alice = userRepository.findByEmail("alice@test.com").orElseThrow();
        assert notificationRepository.countByUserIdAndIsReadFalse(alice.getId()) >= 1;
    }

    /* 16. Notification on accept */
    @Test
    void notificationCreatedOnAccept() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        User investor = userRepository.findByEmail("inv@test.com").orElseThrow();
        assert notificationRepository.countByUserIdAndIsReadFalse(investor.getId()) >= 1;
    }

    /* 17. Notification on reject */
    @Test
    void notificationCreatedOnReject() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult res = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long requestId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + requestId + "/reject")
                .header("Authorization", "Bearer " + startupToken));

        User investor = userRepository.findByEmail("inv@test.com").orElseThrow();
        assert notificationRepository.countByUserIdAndIsReadFalse(investor.getId()) >= 1;
    }
}