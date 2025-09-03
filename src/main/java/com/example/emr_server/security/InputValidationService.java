package com.example.emr_server.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

/**
 * Input validation service to prevent injection attacks
 * Addresses OWASP A03:2021 – Injection
 */
@Service
@Slf4j
public class InputValidationService {

    // Patterns for detecting potential SQL injection
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('|--|;|\\||\\*|%|SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION|SCRIPT)",
        Pattern.CASE_INSENSITIVE
    );

    // Patterns for detecting XSS attempts
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=)",
        Pattern.CASE_INSENSITIVE
    );

    // PESEL validation pattern
    private static final Pattern PESEL_PATTERN = Pattern.compile("^\\d{11}$");

    // Safe characters for medical data
    private static final Pattern SAFE_MEDICAL_TEXT = Pattern.compile("^[a-zA-Z0-9\\s.,;:()\\-+/]*$");

    public boolean isSafeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true; // Empty input is safe
        }

        return !containsSqlInjection(input) && !containsXss(input);
    }

    public boolean containsSqlInjection(String input) {
        if (input == null) return false;
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    public boolean containsXss(String input) {
        if (input == null) return false;
        return XSS_PATTERN.matcher(input).find();
    }

    public boolean isValidPesel(String pesel) {
        if (pesel == null) return false;

        if (!PESEL_PATTERN.matcher(pesel).matches()) {
            return false;
        }

        // PESEL checksum validation
        int[] weights = {1, 3, 7, 9, 1, 3, 7, 9, 1, 3};
        int sum = 0;

        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(pesel.charAt(i)) * weights[i];
        }

        int checksum = (10 - (sum % 10)) % 10;
        return checksum == Character.getNumericValue(pesel.charAt(10));
    }

    public String sanitizeMedicalText(String input) {
        if (input == null) return null;

        // Remove potentially dangerous characters but keep medical text readable
        String sanitized = input.replaceAll("[<>\"'&]", "")
                               .replaceAll("\\s+", " ")
                               .trim();

        // Limit length after sanitization to avoid StringIndexOutOfBoundsException
        return sanitized.substring(0, Math.min(sanitized.length(), 1000));
    }

    public boolean isSafeMedicalText(String input) {
        if (input == null) return true;
        return SAFE_MEDICAL_TEXT.matcher(input).matches() && input.length() <= 1000;
    }

    public void validateAndLog(String input, String context) {
        if (!isSafeInput(input)) {
            log.warn("Potentially malicious input detected in context: {} - Input: {}",
                    context, sanitizeForLog(input));
            throw new SecurityException("Invalid input detected");
        }
    }

    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n\\t]", "_").substring(0, Math.min(input.length(), 100));
    }
}
