package com.ventureai.backend.repository;

import com.ventureai.backend.entity.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StartupRepository extends JpaRepository<Startup, Long> {

    List<Startup> findByUserId(Long userId);

    Optional<Startup> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    List<Startup> findByIndustryIgnoreCase(String industry);

    List<Startup> findByStartupStageIgnoreCase(String startupStage);

    long countByIndustryIgnoreCase(String industry);

    long countByStartupStageIgnoreCase(String startupStage);
}