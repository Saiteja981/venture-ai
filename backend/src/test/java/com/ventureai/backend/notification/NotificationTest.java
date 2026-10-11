package com.ventureai.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureai.backend.dto.RegisterRequest;
import com.ventureai.backend.entity.NotificationType;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.repository.AuditLogRepository;
import com.ventureai.backend.repository.CollaborationRequestRepository;
import com.ventureai.backend.repository.ConversationRepository;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.MessageRepository;
import com.ventureai.backend.repository.NotificationRepository;
import com.ventureai.backend.repository.SavedStartupRepository;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.UserRepository;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.AiFeedbackRepository;
import com.ventureai.backend.service.NotificationService;
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
class NotificationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private CollaborationRequestRepository collaborationRepository;
    @Autowired private StartupRepository startupRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private SavedStartupRepository savedStartupRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private InvestorRepository investorRepository;
    @Autowired private NotificationService notificationService;
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

    private String registerAndGetToken(String name, String email, Role role) throws Exception {
        RegisterRequest req = new RegisterRequest(name, email, "Password123!", role);
        MvcResult res = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private User userByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    @Test
    void createNotificationStoresAndReturnsResponse() throws Exception {
        registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);
        User alice = userByEmail("alice@test.com");

        var dto = notificationService.createNotification(
                alice, "Test", "Hello", NotificationType.SYSTEM, null, null);

        assert dto.id() != null;
        assert "Test".equals(dto.title());
        assert !dto.isRead();
        assert notificationRepository.countByUserIdAndIsReadFalse(alice.getId()) == 1;
    }

    @Test
    void listReturnsOnlyOwnNotifications() throws Exception {
        String aliceToken = registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);
        registerAndGetToken("Bob", "bob@test.com", Role.STARTUP);

        notificationService.createNotification(userByEmail("alice@test.com"),
                "Alice note", "A", NotificationType.SYSTEM, null, null);
        notificationService.createNotification(userByEmail("bob@test.com"),
                "Bob note", "B", NotificationType.SYSTEM, null, null);

        mvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Alice note"));
    }

    @Test
    void unreadCountReflectsOnlyUnread() throws Exception {
        String token = registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);
        User alice = userByEmail("alice@test.com");

        notificationService.createNotification(alice, "N1", "m", NotificationType.SYSTEM, null, null);
        notificationService.createNotification(alice, "N2", "m", NotificationType.SYSTEM, null, null);
        notificationService.createNotification(alice, "N3", "m", NotificationType.SYSTEM, null, null);

        mvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(3));
    }

    @Test
    void markAsReadUpdatesSingleNotification() throws Exception {
        String token = registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);
        User alice = userByEmail("alice@test.com");

        var dto = notificationService.createNotification(alice, "N1", "m",
                NotificationType.SYSTEM, null, null);

        mvc.perform(patch("/api/notifications/" + dto.id() + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        mvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void markAllAsReadClearsUnread() throws Exception {
        String token = registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);
        User alice = userByEmail("alice@test.com");

        notificationService.createNotification(alice, "N1", "m", NotificationType.SYSTEM, null, null);
        notificationService.createNotification(alice, "N2", "m", NotificationType.SYSTEM, null, null);

        mvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        mvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void deleteRemovesOwnNotification() throws Exception {
        String token = registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);
        User alice = userByEmail("alice@test.com");

        var dto = notificationService.createNotification(alice, "N1", "m",
                NotificationType.SYSTEM, null, null);

        mvc.perform(delete("/api/notifications/" + dto.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotReadOrDeleteAnotherUsersNotification() throws Exception {
        registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);
        String bobToken = registerAndGetToken("Bob", "bob@test.com", Role.STARTUP);

        var aliceNote = notificationService.createNotification(userByEmail("alice@test.com"),
                "Private", "m", NotificationType.SYSTEM, null, null);

        mvc.perform(patch("/api/notifications/" + aliceNote.id() + "/read")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/notifications/" + aliceNote.id())
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingNotificationReturns404() throws Exception {
        String token = registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);

        mvc.perform(patch("/api/notifications/9999/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/notifications/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aiEventCreatesNotificationForCurrentUser() throws Exception {
        String token = registerAndGetToken("Alice", "alice@test.com", Role.STARTUP);

        mvc.perform(post("/api/notifications/ai-event")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"AI_POTENTIAL_COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("AI Potential analysis completed"));

        mvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.unread").value(1));
    }
}