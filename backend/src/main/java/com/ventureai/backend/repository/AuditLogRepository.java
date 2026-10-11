package com.ventureai.backend.repository;

import com.ventureai.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {
    // No update/delete methods are intentionally exposed here.
    // The service layer only calls save() and find*() — records are append-only via the API.
}