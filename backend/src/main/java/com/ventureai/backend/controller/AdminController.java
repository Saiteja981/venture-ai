package com.ventureai.backend.controller;

import com.ventureai.backend.dto.*;
import com.ventureai.backend.entity.AuditOutcome;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.service.AdminAiMonitoringService;
import com.ventureai.backend.service.AdminInvestorService;
import com.ventureai.backend.service.AdminStartupService;
import com.ventureai.backend.service.AdminStatsService;
import com.ventureai.backend.service.AdminUserService;
import com.ventureai.backend.service.AuditLogService;
import com.ventureai.backend.service.reports.AdminReportService;
import com.ventureai.backend.service.reports.ReportDownload;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;
    private final AdminAiMonitoringService adminAiMonitoringService;
    private final AdminStartupService adminStartupService;
    private final AdminInvestorService adminInvestorService;
    private final AdminReportService adminReportService;

    public AdminController(AdminStatsService adminStatsService,
                           AdminUserService adminUserService,
                           AuditLogService auditLogService,
                           AdminAiMonitoringService adminAiMonitoringService,
                           AdminStartupService adminStartupService,
                           AdminInvestorService adminInvestorService,
                           AdminReportService adminReportService) {
        this.adminStatsService = adminStatsService;
        this.adminUserService = adminUserService;
        this.auditLogService = auditLogService;
        this.adminAiMonitoringService = adminAiMonitoringService;
        this.adminStartupService = adminStartupService;
        this.adminInvestorService = adminInvestorService;
        this.adminReportService = adminReportService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse getPlatformStats() {
        return adminStatsService.getPlatformStats();
    }

    /* ---------- Users (Phase 21) ---------- */

    @GetMapping("/users")
    public PagedResponse<AdminUserSummaryResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled) {
        return adminUserService.list(page, size, search, role, enabled);
    }

    @GetMapping("/users/{id}")
    public AdminUserDetailResponse getUser(@PathVariable Long id) {
        return adminUserService.get(id);
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserDetailResponse updateUserStatus(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateUserStatusRequest request) {
        boolean newEnabled = request.status() == UserStatus.ACTIVE;
        return adminUserService.updateStatus(id, newEnabled);
    }

    /* ---------- Startups (Phase 24) ---------- */

    @GetMapping("/startups")
    public PagedResponse<AdminStartupSummaryResponse> listStartups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String stage) {
        return adminStartupService.list(page, size, search, industry, stage);
    }

    @GetMapping("/startups/{id}")
    public AdminStartupDetailResponse getStartup(@PathVariable Long id) {
        return adminStartupService.get(id);
    }

    /* ---------- Investors (Phase 24) ---------- */

    @GetMapping("/investors")
    public PagedResponse<AdminInvestorSummaryResponse> listInvestors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String investorType,
            @RequestParam(required = false) String riskPreference) {
        return adminInvestorService.list(page, size, search, investorType, riskPreference);
    }

    @GetMapping("/investors/{id}")
    public AdminInvestorDetailResponse getInvestor(@PathVariable Long id) {
        return adminInvestorService.get(id);
    }

    /* ---------- Audit logs (Phase 21) ---------- */

    @GetMapping("/audit-logs")
    public PagedResponse<AuditLogResponse> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) Long actorId) {
        return auditLogService.list(page, size, action, outcome, actorId);
    }

    /* ---------- AI Monitoring (Phase 24-monitoring) ---------- */

    @GetMapping("/ai/monitoring")
    public AdminAiMonitoringResponse getAiMonitoring(
            @RequestParam(defaultValue = "14") int trendDays) {
        return adminAiMonitoringService.getMonitoring(trendDays);
    }

    /* ---------- Reports (Phase 27) ---------- */

    @GetMapping("/reports/analytics")
    public ResponseEntity<byte[]> exportAnalytics(
            @RequestParam(defaultValue = "pdf") String format) {
        return buildDownload(adminReportService.analytics(format));
    }

    @GetMapping("/reports/audit-logs")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return buildDownload(adminReportService.auditLogs(format, from, to));
    }

    @GetMapping("/reports/ai-executions")
    public ResponseEntity<byte[]> exportAiExecutions(
            @RequestParam(defaultValue = "pdf") String format) {
        return buildDownload(adminReportService.aiExecutionSummary(format));
    }

    private ResponseEntity<byte[]> buildDownload(ReportDownload report) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(report.mediaType());
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + report.filename() + "\"");
        headers.setContentLength(report.bytes().length);
        return ResponseEntity.ok().headers(headers).body(report.bytes());
    }
}