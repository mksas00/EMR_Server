package com.example.emr_server.security;

import com.example.emr_server.exception.ResourceNotFoundException;
import com.example.emr_server.exception.BtgAccessDeniedException;
import com.example.emr_server.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for secure error responses
 * Prevents sensitive data leakage in error messages
 * Addresses OWASP A09:2021 – Security Logging and Monitoring Failures
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        // Log full details securely (no sensitive data in logs)
        log.warn("Resource not found: {} for request: {}",
                sanitizeForLog(ex.getMessage()),
                sanitizeForLog(request.getDescription(false)));

        return createErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied for request: {} - Reason: {}",
                sanitizeForLog(request.getDescription(false)),
                sanitizeForLog(ex.getMessage()));

        return createErrorResponse(HttpStatus.FORBIDDEN, "Access denied", "Insufficient privileges");
    }

    @ExceptionHandler(BtgAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleBtgAccessDenied(
            BtgAccessDeniedException ex, WebRequest request) {

        log.warn("BTG access denied: {} for request: {}",
                sanitizeForLog(ex.getMessage()),
                sanitizeForLog(request.getDescription(false)));

        return createErrorResponse(HttpStatus.FORBIDDEN, "BTG access required",
                "Break-the-glass access is required for this operation");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {

        log.warn("Authentication failed for request: {}",
                sanitizeForLog(request.getDescription(false)));

        return createErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed",
                "Invalid credentials");
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            ValidationException ex, WebRequest request) {

        log.warn("Validation error: {} for request: {}",
                sanitizeForLog(ex.getMessage()),
                sanitizeForLog(request.getDescription(false)));

        return createErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, WebRequest request) {

        // Log full stack trace for debugging but don't expose to client
        log.error("Unexpected error for request: {}",
                sanitizeForLog(request.getDescription(false)), ex);

        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            HttpStatus status, String error, String message) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", ""); // Don't expose internal paths

        return new ResponseEntity<>(response, status);
    }

    /**
     * Sanitize strings for logging to prevent log injection
     * Remove sensitive patterns and control characters
     */
    private String sanitizeForLog(String input) {
        if (input == null) return "null";

        return input
                // Remove potential PESEL/SSN patterns
                .replaceAll("\\b\\d{11}\\b", "[PESEL]")
                // Remove potential credit card patterns
                .replaceAll("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b", "[CARD]")
                // Remove email addresses
                .replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "[EMAIL]")
                // Remove control characters that could cause log injection
                .replaceAll("[\\r\\n\\t]", "_")
                // Limit length to prevent log flooding
                .substring(0, Math.min(input.length(), 200));
    }
}
