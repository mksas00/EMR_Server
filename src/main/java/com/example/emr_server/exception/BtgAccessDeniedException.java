package com.example.emr_server.exception;

/**
 * Exception thrown when BTG (Break-the-Glass) access is required but not granted
 * Used in emergency access scenarios for medical data
 */
public class BtgAccessDeniedException extends RuntimeException {

    public BtgAccessDeniedException(String message) {
        super(message);
    }

    public BtgAccessDeniedException(String patientId, String reason) {
        super(String.format("BTG access denied for patient %s. Reason: %s", patientId, reason));
    }

    public BtgAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
