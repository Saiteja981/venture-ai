package com.ventureai.backend.controller;

import com.ventureai.backend.dto.AiEventRequest;
import com.ventureai.backend.dto.NotificationResponse;
import com.ventureai.backend.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.getCurrentUserNotifications();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.getUnreadCount());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markAsRead(id);
    }

    @PatchMapping("/read-all")
    public Map<String, Long> markAllRead() {
        return Map.of("updated", notificationService.markAllAsRead());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ai-event")
    public NotificationResponse recordAiEvent(@Valid @RequestBody AiEventRequest request) {
        return notificationService.recordAiEvent(request.eventType().name());
    }
}