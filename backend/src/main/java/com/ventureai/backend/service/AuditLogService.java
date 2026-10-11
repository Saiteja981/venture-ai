package com.ventureai.backend.service;

import com.ventureai.backend.dto.AuditLogResponse;
import com.ventureai.backend.dto.PagedResponse;
import com.ventureai.backend.entity.*;
import com.ventureai.backend.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Writes an audit record in its own transaction so it is not rolled back
     * when the caller's transaction later throws (e.g. self-deactivation denial).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, String action, String targetType, Long targetId,
                       AuditOutcome outcome, String details) {
        String email = actor != null ? actor.getEmail() : currentActorEmail();
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .actorEmail(email)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .outcome(outcome)
                .details(details == null || details.length() <= 500
                        ? details : details.substring(0, 500))
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> list(int page, int size, String action,
                                                AuditOutcome outcome, Long actorId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (action != null && !action.isBlank()) {
                ps.add(cb.equal(root.get("action"), action));
            }
            if (outcome != null) {
                ps.add(cb.equal(root.get("outcome"), outcome));
            }
            if (actorId != null) {
                ps.add(cb.equal(root.get("actor").get("id"), actorId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<AuditLogResponse> p = auditLogRepository.findAll(spec, pageable)
                .map(AuditLogResponse::from);

        return PagedResponse.from(p);
    }

    private String currentActorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}