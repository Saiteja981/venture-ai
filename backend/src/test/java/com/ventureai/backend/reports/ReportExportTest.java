package com.ventureai.backend.reports;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportExportTest {

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

    private Long createStartup(String ownerEmail) {
        User u = userRepository.findByEmail(ownerEmail).orElseThrow();
        return startupRepository.save(Startup.builder()
                .user(u).companyName("Test Startup").description("A test")
                .industry("FinTech").businessModel("B2B SaaS").startupStage("SEED")
                .teamSize(5).totalUsers(1000L).customerCount(100L)
                .monthlyRevenue(new BigDecimal("10000"))
                .revenueGrowthRate(new BigDecimal("12.5"))
                .totalFunding(new BigDecimal("500000"))
                .monthlyBurnRate(new BigDecimal("5000"))
                .runwayMonths(new BigDecimal("18"))
                .valuation(new BigDecimal("2000000"))
                .build()).getId();
    }

    private void createInvestorProfile(String email) {
        User u = userRepository.findByEmail(email).orElseThrow();
        investorRepository.save(Investor.builder()
                .user(u).investorName("Inv " + email).organizationName("Org")
                .investorType("VC_FIRM").preferredIndustries("FinTech")
                .preferredStages("SEED").riskPreference("MEDIUM")
                .investmentExperienceYears(5)
                .minimumInvestment(new BigDecimal("25000"))
                .maximumInvestment(new BigDecimal("500000"))
                .build());
    }

    /* ---------- Startup ---------- */

    @Test
    void startupOwnerCanExportProfileAsCsv() throws Exception {
        String token = register("S", "s@test.com", Role.STARTUP);
        createStartup("s@test.com");

        MvcResult res = mvc.perform(get("/api/reports/startup/profile?format=csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(res.getResponse().getContentType().startsWith("text/csv"));
        assertTrue(res.getResponse().getHeader("Content-Disposition").contains(".csv"));
        String body = res.getResponse().getContentAsString();
        assertTrue(body.contains("Company Name"));
        assertTrue(body.contains("Test Startup"));
    }

    @Test
    void startupOwnerCanExportProfileAsPdf() throws Exception {
        String token = register("S", "s@test.com", Role.STARTUP);
        createStartup("s@test.com");

        MvcResult res = mvc.perform(get("/api/reports/startup/profile?format=pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(res.getResponse().getContentType().startsWith("application/pdf"));
        assertTrue(res.getResponse().getHeader("Content-Disposition").contains(".pdf"));
        byte[] bytes = res.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 100, "PDF body should be non-trivial");
        // PDF magic bytes
        assertEquals('%', bytes[0]);
        assertEquals('P', bytes[1]);
        assertEquals('D', bytes[2]);
        assertEquals('F', bytes[3]);
    }

    @Test
    void startupWithoutProfileReturns404() throws Exception {
        String token = register("S", "s@test.com", Role.STARTUP);
        mvc.perform(get("/api/reports/startup/profile?format=csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void aiAnalysisWithNoExecutionsReturnsEmptyReport() throws Exception {
        String token = register("S", "s@test.com", Role.STARTUP);
        createStartup("s@test.com");

        MvcResult res = mvc.perform(get("/api/reports/startup/ai-analysis?format=csv")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String body = res.getResponse().getContentAsString();
        assertTrue(body.contains("Executed At"));   // header only
    }

    /* ---------- Investor ---------- */

    @Test
    void investorCanExportSavedStartupsCsv() throws Exception {
        String inv = register("I", "i@test.com", Role.INVESTOR);
        register("S", "s@test.com", Role.STARTUP);
        createInvestorProfile("i@test.com");
        Long startupId = createStartup("s@test.com");

        mvc.perform(post("/api/investors/me/saved-startups/" + startupId)
                .header("Authorization", "Bearer " + inv));

        MvcResult res = mvc.perform(get("/api/reports/investor/saved-startups?format=csv")
                        .header("Authorization", "Bearer " + inv))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(res.getResponse().getContentAsString().contains("Test Startup"));
    }

    @Test
    void investorCanExportPortfolioPdf() throws Exception {
        String inv = register("I", "i@test.com", Role.INVESTOR);
        String sToken = register("S", "s@test.com", Role.STARTUP);
        createInvestorProfile("i@test.com");
        Long startupId = createStartup("s@test.com");

        MvcResult cRes = mvc.perform(post("/api/collaborations")
                        .header("Authorization", "Bearer " + inv)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startupId\":" + startupId + "}"))
                .andReturn();
        long collabId = json.readTree(cRes.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/collaborations/" + collabId + "/accept")
                .header("Authorization", "Bearer " + sToken));

        MvcResult res = mvc.perform(get("/api/reports/investor/portfolio?format=pdf")
                        .header("Authorization", "Bearer " + inv))
                .andExpect(status().isOk())
                .andReturn();
        byte[] bytes = res.getResponse().getContentAsByteArray();
        assertEquals('P', bytes[1]);
    }

    @Test
    void investorWithoutProfileReturns404() throws Exception {
        String inv = register("I", "i@test.com", Role.INVESTOR);
        mvc.perform(get("/api/reports/investor/saved-startups?format=csv")
                        .header("Authorization", "Bearer " + inv))
                .andExpect(status().isNotFound());
    }

    /* ---------- Admin ---------- */

    @Test
    void adminCanExportAnalytics() throws Exception {
        String admin = register("A", "a@test.com", Role.ADMIN);
        MvcResult res = mvc.perform(get("/api/admin/reports/analytics?format=csv")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(res.getResponse().getContentAsString().contains("Total Users"));
    }

    @Test
    void adminCanExportAuditLogsCsv() throws Exception {
        String admin = register("A", "a@test.com", Role.ADMIN);
        MvcResult res = mvc.perform(get("/api/admin/reports/audit-logs?format=csv")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(res.getResponse().getContentAsString().contains("Actor Email"));
    }

    /* ---------- Authorization ---------- */

    @Test
    void startupCannotAccessAdminReports() throws Exception {
        String s = register("S", "s@test.com", Role.STARTUP);
        mvc.perform(get("/api/admin/reports/analytics?format=csv")
                        .header("Authorization", "Bearer " + s))
                .andExpect(status().isForbidden());
    }

    @Test
    void investorCannotAccessStartupReports() throws Exception {
        String i = register("I", "i@test.com", Role.INVESTOR);
        mvc.perform(get("/api/reports/startup/profile?format=csv")
                        .header("Authorization", "Bearer " + i))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedDeniedOnAllReportEndpoints() throws Exception {
        mvc.perform(get("/api/reports/startup/profile?format=csv"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reports/investor/saved-startups?format=csv"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/reports/analytics?format=csv"))
                .andExpect(status().isUnauthorized());
    }

    /* ---------- Sensitive data exclusion ---------- */

    @Test
    void reportsNeverExposePasswordHash() throws Exception {
        String token = register("S", "s@test.com", Role.STARTUP);
        createStartup("s@test.com");

        MvcResult res = mvc.perform(get("/api/reports/startup/profile?format=csv")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String body = res.getResponse().getContentAsString();
        assertFalse(body.contains("$2a$"));
        assertFalse(body.contains("$2b$"));
        assertFalse(body.contains("password"));
        assertFalse(body.contains("token"));
    }
}