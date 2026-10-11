package com.ventureai.backend.repository;

import com.ventureai.backend.entity.Investor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface InvestorRepository extends JpaRepository<Investor, Long>,
        JpaSpecificationExecutor<Investor> {

    Optional<Investor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Investor> findByInvestorTypeIgnoreCase(String investorType);

    List<Investor> findByRiskPreferenceIgnoreCase(String riskPreference);

    List<Investor> findByPreferredLocationIgnoreCase(String preferredLocation);
}