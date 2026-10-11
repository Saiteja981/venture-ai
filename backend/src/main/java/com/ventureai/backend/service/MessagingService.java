package com.ventureai.backend.service;

import com.ventureai.backend.dto.ConversationResponse;
import com.ventureai.backend.dto.MessageResponse;
import com.ventureai.backend.entity.*;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.ConversationRepository;
import com.ventureai.backend.repository.CollaborationRequestRepository;
import com.ventureai.backend.repository.MessageRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CollaborationRequestRepository collaborationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public MessagingService(ConversationRepository conversationRepository,
                            MessageRepository messageRepository,
                            CollaborationRequestRepository collaborationRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.collaborationRepository = collaborationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /* ---------- current user ---------- */

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private boolean isParticipant(Conversation c, Long userId) {
        CollaborationRequest cr = c.getCollaborationRequest();
        return cr.getInvestor().getUser().getId().equals(userId)
                || cr.getStartup().getUser().getId().equals(userId);
    }

    /* ---------- create or get conversation ---------- */

    @Transactional
    public ConversationResponse createOrGetConversation(Long collaborationId) {
        User user = currentUser();
        CollaborationRequest cr = collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found"));

        // Only participants
        boolean isInvestor = cr.getInvestor().getUser().getId().equals(user.getId());
        boolean isStartup = cr.getStartup().getUser().getId().equals(user.getId());
        if (!isInvestor && !isStartup) {
            throw new AccessDeniedException("You are not a participant in this collaboration");
        }

        // Only ACCEPTED collaborations can message
        if (cr.getStatus() != CollaborationStatus.ACCEPTED) {
            throw new IllegalArgumentException(
                    "Messaging is only available for ACCEPTED collaborations (current: " + cr.getStatus() + ")");
        }

        // Try to find existing
        var existing = conversationRepository.findByCollaborationRequestId(collaborationId);
        if (existing.isPresent()) {
            return toResponse(existing.get(), user.getId());
        }

        // Create — handle race with unique constraint
        Conversation c = Conversation.builder()
                .collaborationRequest(cr)
                .build();
        try {
            c = conversationRepository.save(c);
        } catch (DataIntegrityViolationException race) {
            c = conversationRepository.findByCollaborationRequestId(collaborationId)
                    .orElseThrow(() -> race);
        }
        return toResponse(c, user.getId());
    }

    /* ---------- list conversations ---------- */

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations() {
        User user = currentUser();
        return conversationRepository.findAllForUser(user.getId())
                .stream()
                .map(c -> toResponse(c, user.getId()))
                .toList();
    }

    /* ---------- messages ---------- */

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId) {
        User user = currentUser();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        if (!isParticipant(c, user.getId())) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, String rawContent) {
        User user = currentUser();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        if (!isParticipant(c, user.getId())) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }

        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }
        if (content.length() > 4000) {
            throw new IllegalArgumentException("Message is too long (max 4000 characters)");
        }

        Message m = Message.builder()
                .conversation(c)
                .sender(user)
                .content(content)
                .build();
        Message saved = messageRepository.save(m);

        // Update conversation's lastMessageAt
        c.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(c);

        // Notify the other participant (never the sender)
        User other = otherParticipant(c, user.getId());
        notificationService.createNotification(
                other,
                "New message",
                user.getName() + " sent you a message about " + c.getCollaborationRequest().getStartup().getCompanyName() + ".",
                NotificationType.MESSAGE,
                "CONVERSATION",
                c.getId()
        );

        return MessageResponse.from(saved);
    }

    @Transactional
    public int markConversationAsRead(Long conversationId) {
        User user = currentUser();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        if (!isParticipant(c, user.getId())) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
        return messageRepository.markAllAsReadInConversation(
                conversationId, user.getId(), LocalDateTime.now());
    }

    /* ---------- helpers ---------- */

    private User otherParticipant(Conversation c, Long currentUserId) {
        CollaborationRequest cr = c.getCollaborationRequest();
        User investorUser = cr.getInvestor().getUser();
        User startupUser = cr.getStartup().getUser();
        return investorUser.getId().equals(currentUserId) ? startupUser : investorUser;
    }

    private ConversationResponse toResponse(Conversation c, Long currentUserId) {
        CollaborationRequest cr = c.getCollaborationRequest();
        User other = otherParticipant(c, currentUserId);

        var messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
        String preview = messages.isEmpty() ? "" :
                messages.get(messages.size() - 1).getContent();
        if (preview.length() > 120) preview = preview.substring(0, 117) + "...";

        long unread = messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(c.getId(), currentUserId);

        return new ConversationResponse(
                c.getId(),
                cr.getId(),
                cr.getStartup().getId(),
                cr.getStartup().getCompanyName(),
                cr.getInvestor().getId(),
                cr.getInvestor().getInvestorName(),
                other.getName(),
                preview,
                c.getLastMessageAt(),
                unread
        );
    }
}