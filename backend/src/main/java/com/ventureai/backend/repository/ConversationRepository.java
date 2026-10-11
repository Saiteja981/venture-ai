package com.ventureai.backend.repository;

import com.ventureai.backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByCollaborationRequestId(Long collaborationRequestId);

    @Query("""
           SELECT c FROM Conversation c
           WHERE c.collaborationRequest.investor.user.id = :userId
              OR c.collaborationRequest.startup.user.id = :userId
           ORDER BY c.lastMessageAt DESC
           """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);
}