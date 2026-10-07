package com.ventureai.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.LoginRequest;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.repository.InvestorRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private StartupRepository startupRepository;
    @Autowired private InvestorRepository investorRepository;

    @BeforeEach
    void clean() {
        startupRepository.deleteAll();
        investorRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String registerUser(String name, String email, String password, Role role) throws Exception {
        RegisterRequest req = new RegisterRequest(name, email, password, role);
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String loginUser(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    // ---------------------------------------------------------------
    // AUTHENTICATION
    // ---------------------------------------------------------------

    @Test
    void registerStartupUserSucceeds() throws Exception {
        RegisterRequest req = new RegisterRequest("Alice", "alice@test.com", "Password123", Role.STARTUP);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("STARTUP"));
    }

    @Test
    void registerInvestorUserSucceeds() throws Exception {
        RegisterRequest req = new RegisterRequest("Bob", "bob@test.com", "Password123", Role.INVESTOR);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("INVESTOR"));
    }

    @Test
    void loginSucceeds() throws Exception {
        registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        LoginRequest req = new LoginRequest("alice@test.com", "Password123");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginWithWrongPasswordRejected() throws Exception {
        registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        LoginRequest req = new LoginRequest("alice@test.com", "WrongPassword");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailRejected() throws Exception {
        LoginRequest req = new LoginRequest("nobody@test.com", "Password123");
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // JWT
    // ---------------------------------------------------------------

    @Test
    void validJwtAccepted() throws Exception {
        String token = registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        mvc.perform(get("/api/startups/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void expiredJwtRejected() throws Exception {
        // A syntactically valid but tampered token — signature check fails
        String bad = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlQHRlc3QuY29tIiwiZXhwIjoxfQ.aaaa";
        mvc.perform(get("/api/startups/my")
                        .header("Authorization", "Bearer " + bad))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJwtRejected() throws Exception {
        mvc.perform(get("/api/startups/my")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingJwtRejectedOnProtectedEndpoint() throws Exception {
        mvc.perform(get("/api/startups/my"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // AUTHORIZATION
    // ---------------------------------------------------------------

    @Test
    void startupCanAccessStartupOperations() throws Exception {
        String token = registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        mvc.perform(get("/api/startups/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void startupCannotCreateInvestorProfile() throws Exception {
        String token = registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        String validInvestorBody = """
                {"investorName":"X","investorType":"VC_FIRM",
                 "preferredIndustries":"AI","preferredStages":"SEED",
                 "minimumInvestment":1000,"maximumInvestment":100000,
                 "riskPreference":"MEDIUM","investmentExperienceYears":5}
                """;
        mvc.perform(post("/api/investors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validInvestorBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void investorCanAccessInvestorOperations() throws Exception {
        String token = registerUser("Bob", "bob@test.com", "Password123", Role.INVESTOR);
        // 404 because the profile does not exist yet
        mvc.perform(get("/api/investors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void investorCannotCreateStartup() throws Exception {
        String token = registerUser("Bob", "bob@test.com", "Password123", Role.INVESTOR);
        String validStartupBody = """
                {"companyName":"Acme","industry":"AI","businessModel":"SaaS",
                 "startupStage":"SEED","teamSize":5,"totalUsers":100,
                 "monthlyRevenue":1000,"revenueGrowthRate":10,"totalFunding":50000,
                 "monthlyBurnRate":2000,"runwayMonths":18,"customerCount":10}
                """;
        mvc.perform(post("/api/startups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validStartupBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void startupCannotAccessAdminEndpoints() throws Exception {
        String token = registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        // The /api/admin/** path is protected by hasRole('ADMIN').
        // No handler exists, so Spring returns 403 (authorization) before 404 (routing).
        mvc.perform(get("/api/admin/anything")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void investorCannotAccessAdminEndpoints() throws Exception {
        String token = registerUser("Bob", "bob@test.com", "Password123", Role.INVESTOR);
        mvc.perform(get("/api/admin/anything")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // PUBLIC ENDPOINTS
    // ---------------------------------------------------------------

    @Test
    void registerIsPublic() throws Exception {
        // no token, invalid body — 400 (validation), not 401
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginIsPublic() throws Exception {
        // no token, invalid body — 400 (validation), not 401
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------
    // OWNERSHIP
    // ---------------------------------------------------------------

    @Test
    void startupCannotSeeAnotherStartupsById() throws Exception {
        String aliceToken = registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        registerUser("Bob", "bob@test.com", "Password123", Role.STARTUP);
        String bobToken = loginUser("bob@test.com", "Password123");

        String createBody = """
                {"companyName":"Acme","industry":"AI","businessModel":"SaaS",
                 "startupStage":"SEED","teamSize":5,"totalUsers":100,
                 "monthlyRevenue":1000,"revenueGrowthRate":10,"totalFunding":50000,
                 "monthlyBurnRate":2000,"runwayMonths":18,"customerCount":10}
                """;
        MvcResult res = mvc.perform(post("/api/startups")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        long aliceStartupId = json.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();

        // Bob tries to fetch Alice's startup — 404 (not found or not owned)
        mvc.perform(get("/api/startups/" + aliceStartupId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void startupCannotUpdateAnotherStartup() throws Exception {
        String aliceToken = registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        registerUser("Bob", "bob@test.com", "Password123", Role.STARTUP);
        String bobToken = loginUser("bob@test.com", "Password123");

        String createBody = """
                {"companyName":"Acme","industry":"AI","businessModel":"SaaS",
                 "startupStage":"SEED","teamSize":5,"totalUsers":100,
                 "monthlyRevenue":1000,"revenueGrowthRate":10,"totalFunding":50000,
                 "monthlyBurnRate":2000,"runwayMonths":18,"customerCount":10}
                """;
        MvcResult res = mvc.perform(post("/api/startups")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        long aliceStartupId = json.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/startups/" + aliceStartupId)
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void startupCannotDeleteAnotherStartup() throws Exception {
        String aliceToken = registerUser("Alice", "alice@test.com", "Password123", Role.STARTUP);
        registerUser("Bob", "bob@test.com", "Password123", Role.STARTUP);
        String bobToken = loginUser("bob@test.com", "Password123");

        String createBody = """
                {"companyName":"Acme","industry":"AI","businessModel":"SaaS",
                 "startupStage":"SEED","teamSize":5,"totalUsers":100,
                 "monthlyRevenue":1000,"revenueGrowthRate":10,"totalFunding":50000,
                 "monthlyBurnRate":2000,"runwayMonths":18,"customerCount":10}
                """;
        MvcResult res = mvc.perform(post("/api/startups")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        long aliceStartupId = json.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/startups/" + aliceStartupId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }
}