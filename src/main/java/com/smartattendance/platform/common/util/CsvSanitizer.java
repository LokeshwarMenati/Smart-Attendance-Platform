package com.smartattendance.platform.common.util;

/**
 * Sanitizes CSV cell values to protect against Spreadsheet Formula Injection (CSV Injection).
 * Cells starting with '=', '+', '-', '@', '\t', or '\r' are escaped by prepending a single quote.
 */
public final class CsvSanitizer {

    private CsvSanitizer() {
        // Utility class
    }

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String trimmed = value.trim();
        char firstChar = trimmed.charAt(0);
        if (firstChar == '=' || firstChar == '+' || firstChar == '-' || firstChar == '@' || firstChar == '\t' || firstChar == '\r') {
            return "'" + value;
        }

        return value;
    }

    public static String escapeForCsv(String value) {
        String sanitized = sanitize(value);
        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }
}

