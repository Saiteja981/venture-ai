package com.ventureai.backend.messaging;

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
class MessagingTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private StartupRepository startupRepository;
    @Autowired private InvestorRepository investorRepository;
    @Autowired private SavedStartupRepository savedStartupRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private CollaborationRequestRepository collaborationRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private AiFeedbackRepository aiFeedbackRepository;
    @Autowired private AiExecutionRepository aiExecutionRepository;
    @Autowired private AuditLogRepository auditLogRepository;

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

    /* 1. Conversation created for ACCEPTED collaboration */
    @Test
    void conversationCreatedForAcceptedCollaboration() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(status().isOk());

        mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.collaborationRequestId").value(collabId));
    }

    /* 2. Duplicate conversation prevented */
    @Test
    void duplicateConversationReturnsSameOne() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult r1 = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        MvcResult r2 = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + startupToken))
                .andReturn();
        long id1 = json.readTree(r1.getResponse().getContentAsString()).get("id").asLong();
        long id2 = json.readTree(r2.getResponse().getContentAsString()).get("id").asLong();

        org.junit.jupiter.api.Assertions.assertEquals(id1, id2);
    }

    /* 3. PENDING collaboration cannot message */
    @Test
    void pendingCollaborationCannotStartMessaging() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isBadRequest());
    }

    /* 4. REJECTED collaboration cannot message */
    @Test
    void rejectedCollaborationCannotStartMessaging() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/reject")
                .header("Authorization", "Bearer " + startupToken));

        mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isBadRequest());
    }

    /* 5. Investor can access conversation */
    @Test
    void investorCanAccessConversation() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk());
    }

    /* 6. Startup owner can access conversation */
    @Test
    void startupOwnerCanAccessConversation() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(status().isOk());
    }

    /* 7. Unrelated investor forbidden */
    @Test
    void unrelatedInvestorForbidden() throws Exception {
        String investor1 = register("Inv1", "inv1@test.com", Role.INVESTOR);
        String investor2 = register("Inv2", "inv2@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv1@test.com");
        createInvestorProfile("inv2@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investor1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investor1))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + investor2))
                .andExpect(status().isForbidden());
    }

    /* 8. Unrelated startup owner forbidden */
    @Test
    void unrelatedStartupOwnerForbidden() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String aliceToken = register("Alice", "alice@test.com", Role.STARTUP);
        String bobToken = register("Bob", "bob@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");
        createStartup("bob@test.com", "Beta Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + aliceToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    /* 9. Unrelated admin forbidden */
    @Test
    void unrelatedAdminForbidden() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        register("Admin", "admin@test.com", Role.STARTUP);
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        MvcResult loginRes = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"Password123!\"}"))
                .andReturn();
        String adminToken = json.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    /* 10. Blank message rejected */
    @Test
    void blankMessageRejected() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    /* 11. Oversized message rejected */
    @Test
    void oversizedMessageRejected() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        String big = "x".repeat(4001);
        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + big + "\"}"))
                .andExpect(status().isBadRequest());
    }

    /* 12. Message persisted correctly */
    @Test
    void messagePersistedCorrectly() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello from investor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello from investor"));

        mvc.perform(get("/api/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Hello from investor"));
    }

    /* 13. Recipient notification created */
    @Test
    void recipientNotificationCreated() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Hi Alice\"}"));

        User alice = userRepository.findByEmail("alice@test.com").orElseThrow();
        long unreadBefore = notificationRepository.countByUserIdAndIsReadFalse(alice.getId());
        org.junit.jupiter.api.Assertions.assertTrue(unreadBefore >= 1);
    }

    /* 14. Sender not notified about own message */
    @Test
    void senderNotNotifiedAboutOwnMessage() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        User investor = userRepository.findByEmail("inv@test.com").orElseThrow();
        long unreadBefore = notificationRepository.countByUserIdAndIsReadFalse(investor.getId());

        mvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Test\"}"));

        long unreadAfter = notificationRepository.countByUserIdAndIsReadFalse(investor.getId());
        org.junit.jupiter.api.Assertions.assertEquals(unreadBefore, unreadAfter);
    }

    /* 15. Read state and unread counts */
    @Test
    void readStateAndUnreadCounts() throws Exception {
        String investorToken = register("Inv", "inv@test.com", Role.INVESTOR);
        String startupToken = register("Alice", "alice@test.com", Role.STARTUP);
        createInvestorProfile("inv@test.com");
        Long startupId = createStartup("alice@test.com", "Alpha Co");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + startupToken));

        MvcResult conv = mvc.perform(post("/api/collaborations/" + collabId + "/conversation")
                        .header("Authorization", "Bearer " + investorToken))
                .andReturn();
        long convId = json.readTree(conv.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Msg 1\"}"));
        mvc.perform(post("/api/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + investorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Msg 2\"}"));

        mvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(jsonPath("$[0].unreadCount").value(2));

        mvc.perform(patch("/api/conversations/" + convId + "/read")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        mvc.perform(get("/api/conversations")
                        .header("Authorization", "Bearer " + startupToken))
                .andExpect(jsonPath("$[0].unreadCount").value(0));
    }

    /* 16. Unauthenticated denied */
    @Test
    void unauthenticatedDenied() throws Exception {
        mvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/collaborations/1/conversation"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/conversations/1/messages"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/conversations/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}