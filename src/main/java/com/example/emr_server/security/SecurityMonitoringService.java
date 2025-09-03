package com.example.emr_server.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security monitoring service for threat detection
 * Addresses OWASP A09:2021 – Security Logging and Monitoring Failures
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMonitoringService {

    private final Map<String, AtomicInteger> suspiciousActivityCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastSuspiciousActivity = new ConcurrentHashMap<>();

    @Async
    public void logSecurityEvent(SecurityEventType eventType, String userId, String details, String ipAddress) {
        String logEntry = String.format(
            "SECURITY_EVENT | Type: %s | User: %s | IP: %s | Details: %s | Timestamp: %s",
            eventType,
            sanitizeUserId(userId),
            sanitizeIpAddress(ipAddress),
            sanitizeDetails(details),
            LocalDateTime.now()
        );

        switch (eventType) {
            case AUTHENTICATION_FAILURE:
                log.warn("AUTH_FAILURE: {}", logEntry);
                trackSuspiciousActivity(ipAddress);
                break;
            case UNAUTHORIZED_ACCESS_ATTEMPT:
                log.error("UNAUTHORIZED_ACCESS: {}", logEntry);
                trackSuspiciousActivity(ipAddress);
                break;
            case BTG_ACCESS_GRANTED:
                log.warn("BTG_ACCESS: {}", logEntry);
                break;
            case PRIVILEGE_ESCALATION_ATTEMPT:
                log.error("PRIVILEGE_ESCALATION: {}", logEntry);
                alertSecurityTeam(eventType, userId, details);
                break;
            case DATA_ACCESS_VIOLATION:
                log.error("DATA_VIOLATION: {}", logEntry);
                alertSecurityTeam(eventType, userId, details);
                break;
            case SUSPICIOUS_QUERY_PATTERN:
                log.warn("SUSPICIOUS_QUERY: {}", logEntry);
                trackSuspiciousActivity(ipAddress);
                break;
            default:
                log.info("SECURITY_INFO: {}", logEntry);
        }
    }

    private void trackSuspiciousActivity(String identifier) {
        // Handle null identifier gracefully
        if (identifier == null) {
            identifier = "unknown";
        }

        AtomicInteger counter = suspiciousActivityCounters.computeIfAbsent(identifier, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        lastSuspiciousActivity.put(identifier, LocalDateTime.now());

        if (count >= 10) { // Threshold for alerting
            alertSecurityTeam(SecurityEventType.REPEATED_SUSPICIOUS_ACTIVITY, identifier,
                "Multiple suspicious activities detected: " + count);
        }
    }

    private void alertSecurityTeam(SecurityEventType eventType, String identifier, String details) {
        // In production, this would integrate with SIEM, email alerts, etc.
        log.error("SECURITY_ALERT | Event: {} | Identifier: {} | Details: {} | IMMEDIATE_ATTENTION_REQUIRED",
            eventType, identifier, details);
    }

    private String sanitizeUserId(String userId) {
        if (userId == null) return "anonymous";
        return userId.replaceAll("[^a-zA-Z0-9-]", "_").substring(0, Math.min(userId.length(), 50));
    }

    private String sanitizeIpAddress(String ip) {
        if (ip == null) return "unknown";
        // Basic IP validation and anonymization
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") ? ip : "invalid_ip";
    }

    private String sanitizeDetails(String details) {
        if (details == null) return "";
        return details.replaceAll("[\\r\\n\\t]", "_").substring(0, Math.min(details.length(), 200));
    }

    public enum SecurityEventType {
        AUTHENTICATION_FAILURE,
        UNAUTHORIZED_ACCESS_ATTEMPT,
        BTG_ACCESS_GRANTED,
        PRIVILEGE_ESCALATION_ATTEMPT,
        DATA_ACCESS_VIOLATION,
        SUSPICIOUS_QUERY_PATTERN,
        REPEATED_SUSPICIOUS_ACTIVITY,
        SESSION_HIJACKING_ATTEMPT,
        RATE_LIMIT_EXCEEDED
    }
}
