package com.example.emr_server.exception;

/**
 * Exception thrown when input validation fails
 * Used for data validation errors in REST API
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String field, String value, String reason) {
        super(String.format("Validation failed for field '%s' with value '%s': %s", field, value, reason));
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
