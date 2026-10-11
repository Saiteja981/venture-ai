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
class AdminStartupManagementTest {

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

    private Long createStartup(String ownerEmail, String company,
                               String industry, String stage) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        Startup s = Startup.builder()
                .user(owner)
                .companyName(company)
                .description("Test startup description")
                .industry(industry)
                .businessModel("B2B SaaS")
                .startupStage(stage)
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

    @Test
    void adminCanListStartups() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("Alice", "alice@test.com", Role.STARTUP);
        createStartup("alice@test.com", "Alpha Co", "FinTech", "SEED");
        createStartup("alice@test.com", "Beta Co", "SaaS", "PRE_SEED");

        mvc.perform(get("/api/admin/startups")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].companyName").exists())
                .andExpect(jsonPath("$.content[0].industry").exists());
    }

    @Test
    void adminCanSearchStartupsByName() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("Alice", "alice@test.com", Role.STARTUP);
        createStartup("alice@test.com", "Alpha Co", "FinTech", "SEED");
        createStartup("alice@test.com", "Beta Co", "SaaS", "PRE_SEED");

        mvc.perform(get("/api/admin/startups?search=alpha")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("Alpha Co"));
    }

    @Test
    void adminCanFilterByIndustryAndStage() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("Alice", "alice@test.com", Role.STARTUP);
        createStartup("alice@test.com", "Alpha Co", "FinTech", "SEED");
        createStartup("alice@test.com", "Beta Co", "SaaS", "SEED");
        createStartup("alice@test.com", "Gamma Co", "FinTech", "PRE_SEED");

        mvc.perform(get("/api/admin/startups?industry=FinTech")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(2));

        mvc.perform(get("/api/admin/startups?stage=PRE_SEED")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(1));

        mvc.perform(get("/api/admin/startups?industry=FinTech&stage=SEED")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void adminCanGetStartupDetail() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("Alice", "alice@test.com", Role.STARTUP);
        Long id = createStartup("alice@test.com", "Alpha Co", "FinTech", "SEED");

        mvc.perform(get("/api/admin/startups/" + id)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.companyName").value("Alpha Co"))
                .andExpect(jsonPath("$.ownerEmail").value("alice@test.com"))
                .andExpect(jsonPath("$.ownerName").value("Alice"))
                // Sensitive fields must not leak
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.ownerPassword").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void missingStartupReturns404() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        mvc.perform(get("/api/admin/startups/99999")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminForbidden() throws Exception {
        String startup = register("S", "s@test.com", Role.STARTUP);
        mvc.perform(get("/api/admin/startups")
                        .header("Authorization", "Bearer " + startup))
                .andExpect(status().isForbidden());

        String investor = register("I", "i@test.com", Role.INVESTOR);
        mvc.perform(get("/api/admin/startups")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/api/admin/startups"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paginationWorks() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        register("Alice", "alice@test.com", Role.STARTUP);
        for (int i = 0; i < 5; i++) {
            createStartup("alice@test.com", "Company " + i, "FinTech", "SEED");
        }

        mvc.perform(get("/api/admin/startups?page=0&size=2")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void emptyDatasetReturnsEmptyPage() throws Exception {
        String admin = register("Admin", "admin@test.com", Role.ADMIN);
        mvc.perform(get("/api/admin/startups")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}