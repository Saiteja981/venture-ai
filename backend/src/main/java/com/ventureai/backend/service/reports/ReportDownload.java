package com.ventureai.backend.service.reports;

import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Response payload for a report export. Bundles the bytes with the correct
 * Content-Type and a safe, timestamped filename.
 */
public record ReportDownload(
        byte[] bytes,
        String filename,
        MediaType mediaType
) {
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static ReportDownload csv(String baseName, String content) {
        return new ReportDownload(
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                safeFilename(baseName, "csv"),
                new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    public static ReportDownload pdf(String baseName, byte[] bytes) {
        return new ReportDownload(
                bytes,
                safeFilename(baseName, "pdf"),
                MediaType.APPLICATION_PDF
        );
    }

    private static String safeFilename(String baseName, String ext) {
        // Strip anything that isn't alphanumeric, dash, or underscore.
        String safe = baseName == null ? "report"
                : baseName.replaceAll("[^A-Za-z0-9\\-_]", "-");
        if (safe.length() > 60) safe = safe.substring(0, 60);
        if (safe.isBlank()) safe = "report";
        return "ventureai-" + safe + "-" + LocalDateTime.now().format(STAMP) + "." + ext;
    }
}