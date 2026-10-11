package com.ventureai.backend.service;

import com.ventureai.backend.dto.PortfolioStartupResponse;
import com.ventureai.backend.dto.PortfolioSummaryResponse;
import com.ventureai.backend.entity.CollaborationRequest;
import com.ventureai.backend.entity.CollaborationStatus;
import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.CollaborationRequestRepository;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.SavedStartupRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final CollaborationRequestRepository collaborationRepository;
    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;
    private final SavedStartupRepository savedStartupRepository;

    public PortfolioService(CollaborationRequestRepository collaborationRepository,
                            InvestorRepository investorRepository,
                            UserRepository userRepository,
                            SavedStartupRepository savedStartupRepository) {
        this.collaborationRepository = collaborationRepository;
        this.investorRepository = investorRepository;
        this.userRepository = userRepository;
        this.savedStartupRepository = savedStartupRepository;
    }

    private Investor currentInvestor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        if (user.getRole() != Role.INVESTOR) {
            throw new AccessDeniedException("Only investors have a portfolio");
        }
        return investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Investor profile not found. Please create an investor profile first."));
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse summary() {
        Investor investor = currentInvestor();
        List<CollaborationRequest> all =
                collaborationRepository.findByInvestorIdOrderByCreatedAtDesc(investor.getId());

        List<CollaborationRequest> accepted = all.stream()
                .filter(c -> c.getStatus() == CollaborationStatus.ACCEPTED)
                .toList();

        long pendingCount = all.stream()
                .filter(c -> c.getStatus() == CollaborationStatus.PENDING)
                .count();

        List<PortfolioStartupResponse> entries = accepted.stream()
                .map(this::toPortfolioEntry)
                .toList();

        Set<String> industries = entries.stream()
                .map(PortfolioStartupResponse::industry)
                .filter(i -> i != null && !i.isBlank())
                .collect(Collectors.toSet());

        long savedCount = savedStartupRepository.countByInvestorId(investor.getId());

        return new PortfolioSummaryResponse(
                entries.size(),
                pendingCount,
                industries.size(),
                savedCount,
                entries
        );
    }

    private PortfolioStartupResponse toPortfolioEntry(CollaborationRequest c) {
        var st = c.getStartup();
        return new PortfolioStartupResponse(
                c.getId(),
                c.getUpdatedAt(),   // accepted timestamp == updated when accepted
                st.getId(),
                st.getCompanyName(),
                st.getDescription(),
                st.getIndustry(),
                st.getStartupStage(),
                st.getBusinessModel(),
                st.getTeamSize(),
                st.getTotalUsers(),
                st.getCustomerCount(),
                st.getMonthlyRevenue(),
                st.getRevenueGrowthRate(),
                st.getTotalFunding(),
                st.getValuation(),
                st.getWebsite()
        );
    }
}