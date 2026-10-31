package com.ventureai.backend.service.reports;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC-4180-compliant CSV writer.
 *
 * Security: cells beginning with =, +, -, @, tab, or CR are prefixed with a
 * single quote to prevent CSV injection / formula execution when opened in
 * Excel, LibreOffice, or Google Sheets.
 */
public class CsvBuilder {

    private final List<String[]> rows = new ArrayList<>();

    public CsvBuilder row(Object... cells) {
        String[] values = new String[cells.length];
        for (int i = 0; i < cells.length; i++) {
            values[i] = sanitize(cells[i] == null ? "" : cells[i].toString());
        }
        rows.add(values);
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(quote(row[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /* ---------- internal ---------- */

    private static String sanitize(String raw) {
        if (raw.isEmpty()) return raw;
        char first = raw.charAt(0);
        // CSV injection prevention.
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            return "'" + raw;
        }
        return raw;
    }

    private static String quote(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuotes) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}