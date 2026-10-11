package com.ventureai.backend.service;

import com.ventureai.backend.dto.SavedStartupResponse;
import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.SavedStartup;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.SavedStartupRepository;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SavedStartupService {

    private final SavedStartupRepository savedStartupRepository;
    private final InvestorRepository investorRepository;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;

    public SavedStartupService(SavedStartupRepository savedStartupRepository,
                               InvestorRepository investorRepository,
                               StartupRepository startupRepository,
                               UserRepository userRepository) {
        this.savedStartupRepository = savedStartupRepository;
        this.investorRepository = investorRepository;
        this.startupRepository = startupRepository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private Investor currentInvestor() {
        User user = currentUser();
        if (user.getRole() != Role.INVESTOR) {
            throw new AccessDeniedException("Only investors can save startups");
        }
        return investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Investor profile not found. Please create an investor profile first."));
    }

    /* ---------- save ---------- */

    @Transactional
    public SavedStartupResponse save(Long startupId) {
        Investor investor = currentInvestor();
        Startup startup = startupRepository.findById(startupId)
                .orElseThrow(() -> new ResourceNotFoundException("Startup not found"));

        if (savedStartupRepository.existsByInvestorIdAndStartupId(investor.getId(), startup.getId())) {
            throw new IllegalArgumentException("Startup is already saved");
        }

        SavedStartup saved = SavedStartup.builder()
                .investor(investor)
                .startup(startup)
                .build();

        try {
            saved = savedStartupRepository.save(saved);
        } catch (DataIntegrityViolationException race) {
            // Concurrent save — treat as duplicate
            throw new IllegalArgumentException("Startup is already saved");
        }

        return SavedStartupResponse.from(saved);
    }

    /* ---------- list ---------- */

    @Transactional(readOnly = true)
    public List<SavedStartupResponse> list() {
        Investor investor = currentInvestor();
        return savedStartupRepository.findByInvestorIdOrderByCreatedAtDesc(investor.getId())
                .stream()
                .map(SavedStartupResponse::from)
                .toList();
    }

    /* ---------- remove ---------- */

    @Transactional
    public void remove(Long startupId) {
        Investor investor = currentInvestor();
        // Validate startup exists so 404 semantics match save
        if (!startupRepository.existsById(startupId)) {
            throw new ResourceNotFoundException("Startup not found");
        }
        long deleted = savedStartupRepository.deleteByInvestorIdAndStartupId(investor.getId(), startupId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Startup is not in your saved list");
        }
    }

    /* ---------- status ---------- */

    @Transactional(readOnly = true)
    public boolean isSaved(Long startupId) {
        Investor investor = currentInvestor();
        return savedStartupRepository.existsByInvestorIdAndStartupId(investor.getId(), startupId);
    }
}