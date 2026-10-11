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

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminInvestorManagementTest {

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

    private Long createInvestor(String ownerEmail, String name, String org,
                                String type, String risk) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        Investor inv = Investor.builder()
                .user(owner)
                .investorName(name)
                .organizationName(org)
                .investorType(type)
                .preferredIndustries("FinTech,SaaS")
                .preferredStages("SEED,SERIES_A")
                .preferredBusinessModels("B2B SaaS")
                .minimumInvestment(new BigDecimal("25000"))
                .maximumInvestment(new BigDecimal("500000"))
                .riskPreference(risk)
                .investmentExperienceYears(5)
                .portfolioSize(10)
                .preferredLocation("San Francisco")
                .build();
        return investorRepository.save(inv).getId();
    }

    @Test
    void adminCanListInvestors() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("I1", "i1@test.com", Role.INVESTOR);
        register("I2", "i2@test.com", Role.INVESTOR);
        createInvestor("i1@test.com", "Alice Investor", "Alpha Capital", "VC_FIRM", "MEDIUM");
        createInvestor("i2@test.com", "Bob Angel", "Beta Ventures", "ANGEL", "HIGH");

        mvc.perform(get("/api/admin/investors")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminCanSearchInvestorsByNameOrOrg() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("I1", "i1@test.com", Role.INVESTOR);
        register("I2", "i2@test.com", Role.INVESTOR);
        createInvestor("i1@test.com", "Alice Investor", "Alpha Capital", "VC_FIRM", "MEDIUM");
        createInvestor("i2@test.com", "Bob Angel", "Beta Ventures", "ANGEL", "HIGH");

        mvc.perform(get("/api/admin/investors?search=alice")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].investorName").value("Alice Investor"));

        mvc.perform(get("/api/admin/investors?search=beta")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].organizationName").value("Beta Ventures"));
    }

    @Test
    void adminCanFilterByTypeAndRisk() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("I1", "i1@test.com", Role.INVESTOR);
        register("I2", "i2@test.com", Role.INVESTOR);
        register("I3", "i3@test.com", Role.INVESTOR);
        createInvestor("i1@test.com", "A", "Org A", "VC_FIRM", "MEDIUM");
        createInvestor("i2@test.com", "B", "Org B", "ANGEL", "HIGH");
        createInvestor("i3@test.com", "C", "Org C", "VC_FIRM", "HIGH");

        mvc.perform(get("/api/admin/investors?investorType=VC_FIRM")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(2));

        mvc.perform(get("/api/admin/investors?riskPreference=HIGH")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(2));

        mvc.perform(get("/api/admin/investors?investorType=VC_FIRM&riskPreference=HIGH")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void adminCanGetInvestorDetail() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("I1", "i1@test.com", Role.INVESTOR);
        Long id = createInvestor("i1@test.com", "Alice Investor",
                "Alpha Capital", "VC_FIRM", "MEDIUM");

        mvc.perform(get("/api/admin/investors/" + id)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.investorName").value("Alice Investor"))
                .andExpect(jsonPath("$.ownerEmail").value("i1@test.com"))
                // Sensitive fields must not leak
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.ownerPassword").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void missingInvestorReturns404() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        mvc.perform(get("/api/admin/investors/99999")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminForbidden() throws Exception {
        String startup = register("S", "s@test.com", Role.STARTUP);
        mvc.perform(get("/api/admin/investors")
                        .header("Authorization", "Bearer " + startup))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/api/admin/investors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyDatasetReturnsEmptyPage() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        mvc.perform(get("/api/admin/investors")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}