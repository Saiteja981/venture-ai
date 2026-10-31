package com.ventureai.backend.service.reports;

import com.lowagie.text .DocumentException;
import com.ventureai.backend.entity.AiExecution;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.AiExecutionRepository;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StartupReportService {

    private final UserRepository userRepository;
    private final StartupRepository startupRepository;
    private final AiExecutionRepository aiExecutionRepository;

    public StartupReportService(UserRepository userRepository,
                                StartupRepository startupRepository,
                                AiExecutionRepository aiExecutionRepository) {
        this.userRepository = userRepository;
        this.startupRepository = startupRepository;
        this.aiExecutionRepository = aiExecutionRepository;
    }

    @Transactional(readOnly = true)
    public ReportDownload profile(String format) {
        User user = currentUser();
        Startup s = startupRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You have not created a startup profile yet."));

        if ("csv".equalsIgnoreCase(format)) {
            String csv = new CsvBuilder()
                    .row("Field", "Value")
                    .row("Company Name", s.getCompanyName())
                    .row("Description", s.getDescription())
                    .row("Industry", s.getIndustry())
                    .row("Stage", s.getStartupStage())
                    .row("Business Model", s.getBusinessModel())
                    .row("Website", s.getWebsite())
                    .row("Team Size", s.getTeamSize())
                    .row("Total Users", s.getTotalUsers())
                    .row("Customers", s.getCustomerCount())
                    .row("Monthly Revenue", s.getMonthlyRevenue())
                    .row("Revenue Growth %", s.getRevenueGrowthRate())
                    .row("Total Funding", s.getTotalFunding())
                    .row("Monthly Burn", s.getMonthlyBurnRate())
                    .row("Runway (months)", s.getRunwayMonths())
                    .row("Valuation", s.getValuation())
                    .row("Created At", s.getCreatedAt())
                    .build();
            return ReportDownload.csv("startup-profile", csv);
        }

        try {
            byte[] pdf = new PdfBuilder("Startup Profile Report",
                    "Generated from persisted Venture AI data.")
                    .heading(s.getCompanyName())
                    .keyValue("Industry", s.getIndustry())
                    .keyValue("Stage", s.getStartupStage())
                    .keyValue("Business Model", s.getBusinessModel())
                    .keyValue("Website", s.getWebsite())
                    .heading("Traction & Financials")
                    .keyValue("Team Size", s.getTeamSize())
                    .keyValue("Total Users", s.getTotalUsers())
                    .keyValue("Customers", s.getCustomerCount())
                    .keyValue("Monthly Revenue", fmtMoney(s.getMonthlyRevenue()))
                    .keyValue("Revenue Growth %", s.getRevenueGrowthRate())
                    .keyValue("Total Funding", fmtMoney(s.getTotalFunding()))
                    .keyValue("Monthly Burn", fmtMoney(s.getMonthlyBurnRate()))
                    .keyValue("Runway (months)", s.getRunwayMonths())
                    .keyValue("Valuation", fmtMoney(s.getValuation()))
                    .separator()
                    .keyValue("Created At", s.getCreatedAt())
                    .note("This report contains only data persisted in Venture AI. "
                            + "It is not investment advice.")
                    .close();
            return ReportDownload.pdf("startup-profile", pdf);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render PDF report.", e);
        }
    }

    @Transactional(readOnly = true)
    public ReportDownload aiAnalysis(String format) {
        User user = currentUser();
        if (startupRepository.findByUserId(user.getId()).isEmpty()) {
            throw new ResourceNotFoundException(
                    "You have not created a startup profile yet.");
        }

        List<AiExecution> executions = aiExecutionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());

        if ("csv".equalsIgnoreCase(format)) {
            CsvBuilder b = new CsvBuilder()
                    .row("Executed At", "Task", "Model Version", "Outcome",
                            "Latency (ms)", "Error Category");
            for (AiExecution e : executions) {
                b.row(e.getCreatedAt(),
                        e.getTask(),
                        e.getModelVersion(),
                        e.getOutcome(),
                        e.getLatencyMs(),
                        e.getErrorCategory());
            }
            return ReportDownload.csv("startup-ai-analysis", b.build());
        }

        try {
            PdfBuilder pdf = new PdfBuilder("AI Analysis Execution Report",
                    "Execution metadata recorded for your account.")
                    .heading("Summary")
                    .keyValue("Total executions", executions.size())
                    .keyValue("Successful",
                            executions.stream().filter(e ->
                                    e.getOutcome().name().equals("SUCCESS")).count())
                    .keyValue("Failed",
                            executions.stream().filter(e ->
                                    e.getOutcome().name().equals("FAILED")).count());

            if (executions.isEmpty()) {
                pdf.separator().note("No AI executions have been recorded yet.");
            } else {
                java.util.List<Object[]> rows = new java.util.ArrayList<>();
                for (AiExecution e : executions) {
                    rows.add(new Object[]{
                            e.getCreatedAt(),
                            e.getTask(),
                            e.getModelVersion() == null ? "—" : e.getModelVersion(),
                            e.getOutcome(),
                            e.getLatencyMs(),
                    });
                }
                pdf.heading("Executions")
                        .table(new String[]{"When", "Task", "Model", "Outcome", "Latency (ms)"}, rows);
            }

            pdf.separator()
                    .note("This report lists execution metadata only. Prediction scores "
                            + "and pitch-deck contents are not persisted by the platform and "
                            + "are therefore not included. No AI output is fabricated.")
                    .note("Not investment advice.");

            return ReportDownload.pdf("startup-ai-analysis", pdf.close());
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render PDF report.", e);
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found"));
    }

    private static String fmtMoney(BigDecimal v) {
        return v == null ? "—" : "$" + v.toPlainString();
    }
}