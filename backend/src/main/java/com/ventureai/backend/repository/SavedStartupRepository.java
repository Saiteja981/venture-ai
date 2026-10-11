package com.ventureai.backend.repository;

import com.ventureai.backend.entity.SavedStartup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedStartupRepository extends JpaRepository<SavedStartup, Long> {

    List<SavedStartup> findByInvestorIdOrderByCreatedAtDesc(Long investorId);

    Optional<SavedStartup> findByInvestorIdAndStartupId(Long investorId, Long startupId);

    boolean existsByInvestorIdAndStartupId(Long investorId, Long startupId);

    long deleteByInvestorIdAndStartupId(Long investorId, Long startupId);

    long countByInvestorId(Long investorId);
}