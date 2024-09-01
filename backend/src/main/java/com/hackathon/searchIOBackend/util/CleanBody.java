package com.hackathon.searchIOBackend.util;

import org.springframework.stereotype.Service;

@Service
public class CleanBody {

    public String cleanText(String text) {
        // Remove HTML tags and excess whitespace
        return text.replaceAll("\\<.*?\\>", "").replaceAll("\\s+", " ").trim();
    }

    public String escapeJson(String text) {
        // Escape characters to be JSON-compliant
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public String escapeHtml(String text) {
        // Escape HTML special characters
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
