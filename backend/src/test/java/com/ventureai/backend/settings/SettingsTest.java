package com.ventureai.backend.settings;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettingsTest {

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

    /* ---------- Preferences ---------- */

    @Test
    void defaultsAreAllTrue() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);

        mvc.perform(get("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifyCollab").value(true))
                .andExpect(jsonPath("$.notifyMessages").value(true))
                .andExpect(jsonPath("$.notifyAiEvents").value(true))
                .andExpect(jsonPath("$.notifyUpdates").value(true));
    }

    @Test
    void updatePersists() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);

        mvc.perform(put("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyCollab\":false,\"notifyMessages\":true," +
                                "\"notifyAiEvents\":false,\"notifyUpdates\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifyCollab").value(false))
                .andExpect(jsonPath("$.notifyAiEvents").value(false));

        mvc.perform(get("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.notifyCollab").value(false))
                .andExpect(jsonPath("$.notifyMessages").value(true))
                .andExpect(jsonPath("$.notifyAiEvents").value(false));
    }

    @Test
    void missingFieldIsRejected() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);
        mvc.perform(put("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyCollab\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preferencesAreUserScoped() throws Exception {
        String a = register("A", "a@test.com", Role.STARTUP);
        String b = register("B", "b@test.com", Role.STARTUP);

        mvc.perform(put("/api/settings/preferences")
                .header("Authorization", "Bearer " + a)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"notifyCollab\":false,\"notifyMessages\":false," +
                        "\"notifyAiEvents\":false,\"notifyUpdates\":false}"));

        mvc.perform(get("/api/settings/preferences")
                        .header("Authorization", "Bearer " + b))
                .andExpect(jsonPath("$.notifyCollab").value(true));
    }

    @Test
    void unauthenticatedRejectedForPreferences() throws Exception {
        mvc.perform(get("/api/settings/preferences"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/settings/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyCollab\":true,\"notifyMessages\":true," +
                                "\"notifyAiEvents\":true,\"notifyUpdates\":true}"))
                .andExpect(status().isUnauthorized());
    }

    /* ---------- Password change ---------- */

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);

        mvc.perform(post("/api/settings/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"WrongOld1!\"," +
                                "\"newPassword\":\"NewPassword1!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordRejectsSameAsCurrent() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);

        mvc.perform(post("/api/settings/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password123!\"," +
                                "\"newPassword\":\"Password123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordRejectsTooShort() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);

        mvc.perform(post("/api/settings/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password123!\"," +
                                "\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordSucceedsAndNewPasswordWorks() throws Exception {
        String token = register("A", "a@test.com", Role.STARTUP);

        mvc.perform(post("/api/settings/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password123!\"," +
                                "\"newPassword\":\"NewPassword456!\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@test.com\",\"password\":\"NewPassword456!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changePasswordUnauthenticatedRejected() throws Exception {
        mvc.perform(post("/api/settings/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Password123!\"," +
                                "\"newPassword\":\"NewPassword456!\"}"))
                .andExpect(status().isUnauthorized());
    }

    /* ---------- Authorization ---------- */

    @Test
    void allRolesCanAccessOwnSettings() throws Exception {
        String s = register("S", "s@test.com", Role.STARTUP);
        String i = register("I", "i@test.com", Role.INVESTOR);
        String a = register("A", "a@test.com", Role.ADMIN);

        for (String token : new String[]{s, i, a}) {
            mvc.perform(get("/api/settings/preferences")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    /* ---------- Investor recommendation-display preferences ---------- */

    @Test
    void investorCanSetRecommendationsPerPage() throws Exception {
        String token = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(put("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyCollab\":true,\"notifyMessages\":true," +
                                "\"notifyAiEvents\":true,\"notifyUpdates\":true," +
                                "\"recommendationsPerPage\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationsPerPage").value(10));

        mvc.perform(get("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.recommendationsPerPage").value(10));
    }

    @Test
    void invalidRecommendationsPerPageRejected() throws Exception {
        String token = register("Inv", "inv@test.com", Role.INVESTOR);

        mvc.perform(put("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyCollab\":true,\"notifyMessages\":true," +
                                "\"notifyAiEvents\":true,\"notifyUpdates\":true," +
                                "\"recommendationsPerPage\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startupRecommendationsPerPageIsIgnored() throws Exception {
        String token = register("S", "s@test.com", Role.STARTUP);

        mvc.perform(put("/api/settings/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyCollab\":true,\"notifyMessages\":true," +
                                "\"notifyAiEvents\":true,\"notifyUpdates\":true," +
                                "\"recommendationsPerPage\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationsPerPage").doesNotExist());
    }
}