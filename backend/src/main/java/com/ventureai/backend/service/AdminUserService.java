package com.ventureai.backend.service;

import com.ventureai.backend.dto.*;
import com.ventureai.backend.entity.*;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AdminUserService(UserRepository userRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserSummaryResponse> list(int page, int size, String search,
                                                        Role role, Boolean enabled) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            if (role != null) ps.add(cb.equal(root.get("role"), role));
            if (enabled != null) ps.add(cb.equal(root.get("enabled"), enabled));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<AdminUserSummaryResponse> p = userRepository.findAll(spec, pageable)
                .map(AdminUserSummaryResponse::from);
        return PagedResponse.from(p);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse get(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return AdminUserDetailResponse.from(u);
    }

    @Transactional
    public AdminUserDetailResponse updateStatus(Long targetId, boolean newEnabled) {
        User actor = currentActor();
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (actor.getId().equals(target.getId()) && !newEnabled) {
            auditLogService.record(actor, "USER_DEACTIVATE", "USER", targetId,
                    AuditOutcome.DENIED, "Self-deactivation blocked");
            throw new IllegalStateException("You cannot deactivate your own account");
        }

        if (target.getRole() == Role.ADMIN && !newEnabled) {
            long activeAdmins = userRepository.countByRoleAndEnabled(Role.ADMIN, true);
            if (activeAdmins <= 1) {
                auditLogService.record(actor, "USER_DEACTIVATE", "USER", targetId,
                        AuditOutcome.DENIED, "Would remove the last active admin");
                throw new IllegalStateException("Cannot deactivate the last active administrator");
            }
        }

        target.setEnabled(newEnabled);
        target = userRepository.save(target);

        auditLogService.record(actor,
                newEnabled ? "USER_ACTIVATE" : "USER_DEACTIVATE",
                "USER", targetId, AuditOutcome.SUCCESS,
                "Enabled changed to " + newEnabled);

        return AdminUserDetailResponse.from(target);
    }

    private User currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}