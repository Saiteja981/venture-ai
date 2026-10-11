package com.ventureai.backend.controller;

import com.ventureai.backend.dto.ConversationResponse;
import com.ventureai.backend.dto.MessageResponse;
import com.ventureai.backend.dto.SendMessageRequest;
import com.ventureai.backend.service.MessagingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> listConversations() {
        return messagingService.listConversations();
    }

    @PostMapping("/collaborations/{collaborationId}/conversation")
    public ConversationResponse createOrGetConversation(@PathVariable Long collaborationId) {
        return messagingService.createOrGetConversation(collaborationId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageResponse> getMessages(@PathVariable Long conversationId) {
        return messagingService.getMessages(conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public MessageResponse sendMessage(@PathVariable Long conversationId,
                                       @Valid @RequestBody SendMessageRequest request) {
        return messagingService.sendMessage(conversationId, request.content());
    }

    @PatchMapping("/conversations/{conversationId}/read")
    public Map<String, Integer> markAsRead(@PathVariable Long conversationId) {
        return Map.of("updated", messagingService.markConversationAsRead(conversationId));
    }
}