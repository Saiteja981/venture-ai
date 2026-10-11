package com.ventureai.backend.repository;

import com.ventureai.backend.entity.CollaborationRequest;
import com.ventureai.backend.entity.CollaborationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollaborationRequestRepository extends JpaRepository<CollaborationRequest, Long> {

    List<CollaborationRequest> findByInvestorIdOrderByCreatedAtDesc(Long investorId);

    List<CollaborationRequest> findByStartupIdOrderByCreatedAtDesc(Long startupId);

    Optional<CollaborationRequest> findByIdAndInvestorId(Long id, Long investorId);

    Optional<CollaborationRequest> findByIdAndStartupId(Long id, Long startupId);

    List<CollaborationRequest> findByInvestorIdAndStartupIdAndStatusIn(
            Long investorId, Long startupId, List<CollaborationStatus> statuses);

    Optional<CollaborationRequest> findFirstByInvestorIdAndStartupIdOrderByCreatedAtDesc(
            Long investorId, Long startupId);
}