package com.example.emr_server.exception;

/**
 * Exception thrown when a requested resource is not found
 * Used for REST API error handling with appropriate HTTP status codes
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Object id) {
        super(String.format("%s with id %s not found", resourceType, id));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
