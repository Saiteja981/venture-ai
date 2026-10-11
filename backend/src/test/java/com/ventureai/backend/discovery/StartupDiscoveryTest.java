package com.ventureai.backend.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.NotificationRepository;
import com.ventureai.backend.repository.CollaborationRequestRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.ventureai.backend.repository.ConversationRepository;
import com.ventureai.backend.repository.MessageRepository;
import com.ventureai.backend.repository.SavedStartupRepository;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.AiFeedbackRepository;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StartupDiscoveryTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private StartupRepository startupRepository;
    @Autowired private InvestorRepository investorRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private SavedStartupRepository savedStartupRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private NotificationRepository notificationRepository;
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

    private void seedStartup(String ownerEmail, String company, String industry,
                             String stage, String businessModel,
                             BigDecimal funding, BigDecimal revenue,
                             Integer teamSize) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();
        Startup s = Startup.builder()
                .user(owner)
                .companyName(company)
                .description(company + " is a " + industry + " company.")
                .industry(industry)
                .businessModel(businessModel)
                .startupStage(stage)
                .teamSize(teamSize)
                .totalUsers(1000L)
                .customerCount(100L)
                .monthlyRevenue(revenue)
                .revenueGrowthRate(new BigDecimal("15.5"))
                .totalFunding(funding)
                .monthlyBurnRate(new BigDecimal("5000"))
                .runwayMonths(new BigDecimal("18"))
                .valuation(new BigDecimal("2000000"))
                .build();
        startupRepository.save(s);
    }

    private void seedFixtures() throws Exception {
        register("Alice", "alice@test.com", Role.STARTUP);
        register("Bob", "bob@test.com", Role.STARTUP);
        register("Carol", "carol@test.com", Role.STARTUP);

        seedStartup("alice@test.com", "FinTech Rocket", "FinTech",
                "PRE_SEED", "B2B SaaS",
                new BigDecimal("500000"), new BigDecimal("25000"), 8);
        seedStartup("bob@test.com", "HealthAI", "HealthTech",
                "SEED", "B2B SaaS",
                new BigDecimal("2000000"), new BigDecimal("80000"), 15);
        seedStartup("carol@test.com", "ShopEasy", "E-Commerce",
                "SEED", "Marketplace",
                new BigDecimal("100000"), new BigDecimal("5000"), 4);
    }

    @Test
    void discoverReturnsAllStartupsWhenNoFilters() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void searchByCompanyName() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?search=HealthAI")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("HealthAI"));
    }

    @Test
    void searchIsCaseInsensitive() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?search=healthai")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filterByIndustry() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?industry=FinTech")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].industry").value("FinTech"));
    }

    @Test
    void filterByStage() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?startupStage=SEED")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByFundingRange() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?minFunding=400000&maxFunding=1000000")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("FinTech Rocket"));
    }

    @Test
    void multipleFiltersCombine() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?startupStage=SEED&businessModel=Marketplace")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("ShopEasy"));
    }

    @Test
    void sortByFundingHigh() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?sort=funding-high")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].companyName").value("HealthAI"));
    }

    @Test
    void paginationWorks() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?page=0&size=2")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void emptyResultWhenNoMatch() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(get("/api/startups/discover?search=zzz-nonexistent")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void investorCanDiscover() throws Exception {
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);
        mvc.perform(get("/api/startups/discover")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDiscover() throws Exception {
        register("Admin", "admin@test.com", Role.STARTUP);
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = json.readTree(res.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/api/startups/discover")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void startupUserForbiddenFromDiscovery() throws Exception {
        String startup = register("Alice", "alice@test.com", Role.STARTUP);
        mvc.perform(get("/api/startups/discover")
                        .header("Authorization", "Bearer " + startup))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mvc.perform(get("/api/startups/discover"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerInfoNotExposed() throws Exception {
        seedFixtures();
        String investor = register("Inv", "inv@test.com", Role.INVESTOR);

        String body = mvc.perform(get("/api/startups/discover")
                        .header("Authorization", "Bearer " + investor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"userId\""), "userId leaked");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"user\""),   "user object leaked");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"password\""), "password leaked");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"email\""),  "email leaked");
    }
}