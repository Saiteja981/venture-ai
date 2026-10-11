package com.ventureai.backend.service;

import com.ventureai.backend.dto.NotificationResponse;
import com.ventureai.backend.entity.Notification;
import com.ventureai.backend.entity.NotificationType;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.NotificationRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /* ---------- Current user resolution ---------- */

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    /* ---------- Reusable internal creation ---------- */

    /**
     * Create a notification for a specific user.
     * This method is public so future modules (collaboration, messaging, admin actions)
     * can trigger notifications without duplicating logic.
     */
    @Transactional
    public NotificationResponse createNotification(User recipient,
                                                   String title,
                                                   String message,
                                                   NotificationType type,
                                                   String relatedEntityType,
                                                   Long relatedEntityId) {
        Notification n = Notification.builder()
                .user(recipient)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .read(false)
                .build();
        return NotificationResponse.from(notificationRepository.save(n));
    }

    /* ---------- Current-user operations ---------- */

    @Transactional(readOnly = true)
    public List<NotificationResponse> getCurrentUserNotifications() {
        User user = currentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = currentUser();
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User user = currentUser();
        Notification n = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.isRead()) {
            n.setRead(true);
            notificationRepository.save(n);
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public long markAllAsRead() {
        User user = currentUser();
        List<Notification> unread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        User user = currentUser();
        long deleted = notificationRepository.deleteByIdAndUserId(notificationId, user.getId());
        if (deleted == 0) {
            throw new ResourceNotFoundException("Notification not found");
        }
    }

    /* ---------- Enumerated AI-event notifications ---------- */

    @Transactional
    public NotificationResponse recordAiEvent(String eventTypeName) {
        User user = currentUser();
        String title;
        String message;
        String relatedType;

        switch (eventTypeName) {
            case "AI_POTENTIAL_COMPLETED" -> {
                title = "AI Potential analysis completed";
                message = "Your startup's AI potential and risk assessment is ready to view.";
                relatedType = "AI_POTENTIAL";
            }
            case "PITCH_DECK_ANALYZED" -> {
                title = "Pitch deck analysis completed";
                message = "Your pitch deck has been analyzed. Section completeness and information gaps are available.";
                relatedType = "PITCH_DECK";
            }
            case "RECOMMENDATIONS_LOADED" -> {
                title = "New startup recommendations available";
                message = "Your latest AI-powered startup recommendations have been generated.";
                relatedType = "RECOMMENDATIONS";
            }
            default -> throw new IllegalArgumentException("Unknown AI event type");
        }

        return createNotification(
                user,
                title,
                message,
                NotificationType.AI_RECOMMENDATION,
                relatedType,
                null
        );
    }
}