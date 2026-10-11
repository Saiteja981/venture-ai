package com.ventureai.backend.service;

import com.ventureai.backend.dto.CollaborationRequestResponse;
import com.ventureai.backend.entity.*;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.CollaborationRequestRepository;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CollaborationService {

    private final CollaborationRequestRepository collaborationRepository;
    private final InvestorRepository investorRepository;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CollaborationService(CollaborationRequestRepository collaborationRepository,
                                InvestorRepository investorRepository,
                                StartupRepository startupRepository,
                                UserRepository userRepository,
                                NotificationService notificationService) {
        this.collaborationRepository = collaborationRepository;
        this.investorRepository = investorRepository;
        this.startupRepository = startupRepository;
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

    /* ---------- create ---------- */

    @Transactional
    public CollaborationRequestResponse createRequest(Long startupId, String message) {
        User user = currentUser();
        if (user.getRole() != Role.INVESTOR) {
            throw new AccessDeniedException("Only investors can create collaboration requests");
        }
        Investor investor = investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Investor profile not found. Please create an investor profile first."));
        Startup startup = startupRepository.findById(startupId)
                .orElseThrow(() -> new ResourceNotFoundException("Startup not found"));

        // Duplicate prevention: block if PENDING or ACCEPTED already exists
        List<CollaborationRequest> active = collaborationRepository
                .findByInvestorIdAndStartupIdAndStatusIn(
                        investor.getId(),
                        startup.getId(),
                        List.of(CollaborationStatus.PENDING, CollaborationStatus.ACCEPTED));

        if (!active.isEmpty()) {
            CollaborationRequest existing = active.get(0);
            if (existing.getStatus() == CollaborationStatus.PENDING) {
                throw new IllegalArgumentException("A pending collaboration request already exists for this startup");
            }
            throw new IllegalArgumentException("You are already collaborating with this startup");
        }

        CollaborationRequest req = CollaborationRequest.builder()
                .investor(investor)
                .startup(startup)
                .message(message != null ? message.trim() : null)
                .status(CollaborationStatus.PENDING)
                .build();

        CollaborationRequest saved = collaborationRepository.save(req);

        // Notify startup owner
        User startupOwner = startup.getUser();
        notificationService.createNotification(
                startupOwner,
                "New collaboration request",
                investor.getInvestorName() + " is interested in " + startup.getCompanyName() + ".",
                NotificationType.COLLABORATION,
                "COLLABORATION_REQUEST",
                saved.getId()
        );

        return CollaborationRequestResponse.from(saved);
    }

    /* ---------- reads ---------- */

    @Transactional(readOnly = true)
    public List<CollaborationRequestResponse> getInvestorRequests() {
        User user = currentUser();
        if (user.getRole() != Role.INVESTOR) {
            throw new AccessDeniedException("Only investors can view their collaboration requests");
        }
        Investor investor = investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Investor profile not found"));
        return collaborationRepository.findByInvestorIdOrderByCreatedAtDesc(investor.getId())
                .stream().map(CollaborationRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CollaborationRequestResponse> getStartupRequests() {
        User user = currentUser();
        if (user.getRole() != Role.STARTUP) {
            throw new AccessDeniedException("Only startup owners can view collaboration requests");
        }
        List<Startup> owned = startupRepository.findByUserId(user.getId());
        if (owned.isEmpty()) return List.of();
        List<Long> ids = owned.stream().map(Startup::getId).toList();
        return ids.stream()
                .flatMap(id -> collaborationRepository.findByStartupIdOrderByCreatedAtDesc(id).stream())
                .map(CollaborationRequestResponse::from)
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CollaborationRequestResponse getRequestById(Long id) {
        User user = currentUser();
        CollaborationRequest req = collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found"));

        boolean isInvestorOwner = req.getInvestor().getUser().getId().equals(user.getId());
        boolean isStartupOwner  = req.getStartup().getUser().getId().equals(user.getId());
        if (!isInvestorOwner && !isStartupOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You do not have access to this collaboration request");
        }
        return CollaborationRequestResponse.from(req);
    }

    /* ---------- state transitions ---------- */

    @Transactional
    public CollaborationRequestResponse acceptRequest(Long id) {
        User user = currentUser();
        CollaborationRequest req = collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found"));

        if (user.getRole() != Role.STARTUP || !req.getStartup().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the startup owner can accept this request");
        }
        if (req.getStatus() != CollaborationStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING requests can be accepted");
        }

        req.setStatus(CollaborationStatus.ACCEPTED);
        CollaborationRequest saved = collaborationRepository.save(req);

        User investorUser = req.getInvestor().getUser();
        notificationService.createNotification(
                investorUser,
                "Collaboration request accepted",
                req.getStartup().getCompanyName() + " accepted your collaboration request.",
                NotificationType.COLLABORATION,
                "COLLABORATION_REQUEST",
                saved.getId()
        );
        return CollaborationRequestResponse.from(saved);
    }

    @Transactional
    public CollaborationRequestResponse rejectRequest(Long id) {
        User user = currentUser();
        CollaborationRequest req = collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found"));

        if (user.getRole() != Role.STARTUP || !req.getStartup().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the startup owner can reject this request");
        }
        if (req.getStatus() != CollaborationStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING requests can be rejected");
        }

        req.setStatus(CollaborationStatus.REJECTED);
        CollaborationRequest saved = collaborationRepository.save(req);

        User investorUser = req.getInvestor().getUser();
        notificationService.createNotification(
                investorUser,
                "Collaboration request declined",
                req.getStartup().getCompanyName() + " declined your collaboration request.",
                NotificationType.COLLABORATION,
                "COLLABORATION_REQUEST",
                saved.getId()
        );
        return CollaborationRequestResponse.from(saved);
    }

    @Transactional
    public CollaborationRequestResponse withdrawRequest(Long id) {
        User user = currentUser();
        CollaborationRequest req = collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found"));

        if (user.getRole() != Role.INVESTOR || !req.getInvestor().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the investor who created this request can withdraw it");
        }
        if (req.getStatus() != CollaborationStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING requests can be withdrawn");
        }

        req.setStatus(CollaborationStatus.WITHDRAWN);
        CollaborationRequest saved = collaborationRepository.save(req);

        // Optional notification to startup owner
        User startupOwner = req.getStartup().getUser();
        notificationService.createNotification(
                startupOwner,
                "Collaboration request withdrawn",
                req.getInvestor().getInvestorName() + " withdrew their collaboration request.",
                NotificationType.COLLABORATION,
                "COLLABORATION_REQUEST",
                saved.getId()
        );
        return CollaborationRequestResponse.from(saved);
    }
}