package com.ventureai.backend.service.reports;

import com.lowagie.text.DocumentException;
import com.ventureai.backend.dto.AdminStatsResponse;
import com.ventureai.backend.entity.AiExecution;
import com.ventureai.backend.entity.AuditLog;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.AuditLogRepository;
import com.ventureai.backend.repository.UserRepository;
import com.ventureai.backend.service.AdminStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportService {

    private final AdminStatsService adminStatsService;
    private final AiExecutionRepository aiExecutionRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AdminReportService(AdminStatsService adminStatsService,
                              AiExecutionRepository aiExecutionRepository,
                              AuditLogRepository auditLogRepository,
                              UserRepository userRepository) {
        this.adminStatsService = adminStatsService;
        this.aiExecutionRepository = aiExecutionRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ReportDownload analytics(String format) {
        AdminStatsResponse stats = adminStatsService.getPlatformStats();

        if ("csv".equals(format)) {
            CsvBuilder b = new CsvBuilder()
                    .row("Metric", "Value")
                    .row("Total Users", stats.totalUsers())
                    .row("Total Startups", stats.totalStartups())
                    .row("Total Investors", stats.totalInvestors())
                    .row("Total Admins", stats.totalAdmins())
                    .row("Total AI Executions", stats.totalAiExecutions())
                    .row("", "")
                    .row("Users by Role", "");

            for (Map.Entry<String, Long> e : stats.usersByRole().entrySet()) {
                b.row("  " + e.getKey(), e.getValue());
            }
            b.row("", "").row("Startups by Industry", "");
            for (Map.Entry<String, Long> e : stats.startupsByIndustry().entrySet()) {
                b.row("  " + e.getKey(), e.getValue());
            }
            return ReportDownload.csv("admin-platform-analytics", b.build());
        }

        try {
            PdfBuilder pdf = new PdfBuilder("Platform Analytics Report",
                    "Generated " + LocalDateTime.now())
                    .heading("Totals")
                    .keyValue("Total Users", stats.totalUsers())
                    .keyValue("Total Startups", stats.totalStartups())
                    .keyValue("Total Investors", stats.totalInvestors())
                    .keyValue("Total Admins", stats.totalAdmins())
                    .keyValue("Total AI Executions", stats.totalAiExecutions())
                    .heading("Users by Role");

            List<Object[]> roleRows = new ArrayList<>();
            stats.usersByRole().forEach((k, v) -> roleRows.add(new Object[]{k, v}));
            pdf.table(new String[]{"Role", "Count"}, roleRows);

            pdf.heading("Startups by Industry");
            List<Object[]> indRows = new ArrayList<>();
            stats.startupsByIndustry().forEach((k, v) -> indRows.add(new Object[]{k, v}));
            pdf.table(new String[]{"Industry", "Count"}, indRows);

            pdf.separator()
                    .note("Operational counts only. No passwords, tokens, or user "
                            + "identifiers are included in this export.")
                    .note("Metrics reflect persisted platform data. Not investment advice.");

            return ReportDownload.pdf("admin-platform-analytics", pdf.close());
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render PDF report.", e);
        }
    }

    @Transactional(readOnly = true)
    public ReportDownload auditLogs(String format, LocalDateTime from, LocalDateTime to) {
        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> from == null || !l.getCreatedAt().isBefore(from))
                .filter(l -> to == null || !l.getCreatedAt().isAfter(to))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(10_000)   // bounded, never unbounded
                .toList();

        // Only CSV is supported for audit logs (volume + readability).
        CsvBuilder b = new CsvBuilder()
                .row("When", "Actor Email", "Action", "Outcome",
                        "Target Type", "Target ID", "Details");
        for (AuditLog l : logs) {
            b.row(l.getCreatedAt(), l.getActorEmail(), l.getAction(),
                    l.getOutcome(), l.getTargetType(), l.getTargetId(),
                    l.getDetails());
        }
        return ReportDownload.csv("admin-audit-logs", b.build());
    }

    @Transactional(readOnly = true)
    public ReportDownload aiExecutionSummary(String format) {
        List<AiExecution> all = aiExecutionRepository.findAll();
        long total = all.size();
        long successful = all.stream()
                .filter(e -> e.getOutcome().name().equals("SUCCESS")).count();
        long failed = total - successful;

        if ("csv".equals(format)) {
            CsvBuilder b = new CsvBuilder()
                    .row("Task", "Outcome", "Model Version", "Count", "Avg Latency (ms)");
            all.stream()
                    .collect(java.util.stream.Collectors.groupingBy(e ->
                            new Object[]{e.getTask(), e.getOutcome(), e.getModelVersion()}))
                    .forEach((key, list) -> {
                        Object[] k = (Object[]) key;
                        long avg = Math.round(list.stream()
                                .filter(e -> e.getLatencyMs() != null)
                                .mapToLong(AiExecution::getLatencyMs)
                                .average().orElse(0));
                        b.row(k[0], k[1], k[2], list.size(), avg);
                    });
            return ReportDownload.csv("admin-ai-executions", b.build());
        }

        try {
            PdfBuilder pdf = new PdfBuilder("AI Execution Summary",
                    "Aggregated operational metrics for all AI executions.")
                    .heading("Overview")
                    .keyValue("Total executions", total)
                    .keyValue("Successful", successful)
                    .keyValue("Failed", failed);

            if (total > 0) {
                List<Object[]> rows = new ArrayList<>();
                all.stream()
                        .collect(java.util.stream.Collectors.groupingBy(e ->
                                new Object[]{e.getTask(), e.getOutcome(), e.getModelVersion()}))
                        .forEach((key, list) -> {
                            Object[] k = (Object[]) key;
                            long avg = Math.round(list.stream()
                                    .filter(e -> e.getLatencyMs() != null)
                                    .mapToLong(AiExecution::getLatencyMs)
                                    .average().orElse(0));
                            rows.add(new Object[]{k[0], k[1],
                                    k[2] == null ? "—" : k[2], list.size(), avg});
                        });
                pdf.heading("Breakdown")
                        .table(new String[]{"Task", "Outcome", "Model", "Count",
                                "Avg Latency (ms)"}, rows);
            }

            pdf.separator()
                    .note("Operational metrics only. These are not model accuracy "
                            + "measurements. Accuracy, precision, and recall require "
                            + "labeled evaluation data and are not derivable from "
                            + "execution logs.");

            return ReportDownload.pdf("admin-ai-executions", pdf.close());
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render PDF report.", e);
        }
    }
}