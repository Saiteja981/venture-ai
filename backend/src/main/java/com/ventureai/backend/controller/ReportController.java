package com.ventureai.backend.controller;

import com.ventureai.backend.service.reports.InvestorReportService;
import com.ventureai.backend.service.reports.ReportDownload;
import com.ventureai.backend.service.reports.StartupReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final StartupReportService startupReports;
    private final InvestorReportService investorReports;

    public ReportController(StartupReportService startupReports,
                            InvestorReportService investorReports) {
        this.startupReports = startupReports;
        this.investorReports = investorReports;
    }

    /* ---------- Startup ---------- */

    @PreAuthorize("hasRole('STARTUP')")
    @GetMapping("/startup/profile")
    public ResponseEntity<byte[]> startupProfile(
            @RequestParam(defaultValue = "pdf") String format) {
        return download(startupReports.profile(format));
    }

    @PreAuthorize("hasRole('STARTUP')")
    @GetMapping("/startup/ai-analysis")
    public ResponseEntity<byte[]> startupAiAnalysis(
            @RequestParam(defaultValue = "pdf") String format) {
        return download(startupReports.aiAnalysis(format));
    }

    /* ---------- Investor ---------- */

    @PreAuthorize("hasRole('INVESTOR')")
    @GetMapping("/investor/saved-startups")
    public ResponseEntity<byte[]> investorSavedStartups(
            @RequestParam(defaultValue = "csv") String format) {
        return download(investorReports.savedStartups(format));
    }

    @PreAuthorize("hasRole('INVESTOR')")
    @GetMapping("/investor/portfolio")
    public ResponseEntity<byte[]> investorPortfolio(
            @RequestParam(defaultValue = "csv") String format) {
        return download(investorReports.portfolio(format));
    }

    /* ---------- shared ---------- */

    private ResponseEntity<byte[]> download(ReportDownload report) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(report.mediaType());
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + report.filename() + "\"");
        headers.setContentLength(report.bytes().length);
        return ResponseEntity.ok().headers(headers).body(report.bytes());
    }
}