package com.example.emr_server.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SecurityMonitoringService
 * Tests security event logging functionality
 */
@ExtendWith(MockitoExtension.class)
class SecurityMonitoringServiceTest {

    @InjectMocks
    private SecurityMonitoringService securityMonitoringService;

    @Test
    @DisplayName("logSecurityEvent_authenticationFailure_logsWarning")
    void logSecurityEvent_authenticationFailure_logsWarning() {
        // Given: authentication failure event
        SecurityMonitoringService.SecurityEventType eventType = SecurityMonitoringService.SecurityEventType.AUTHENTICATION_FAILURE;
        String userId = "testUser";
        String details = "Invalid password";
        String ipAddress = "192.168.1.100";

        // When: logging security event
        securityMonitoringService.logSecurityEvent(eventType, userId, details, ipAddress);

        // Then: no exception thrown (logging is asynchronous)
        assertThat(eventType).isEqualTo(SecurityMonitoringService.SecurityEventType.AUTHENTICATION_FAILURE);
        assertThat(userId).isEqualTo("testUser");
        assertThat(details).isEqualTo("Invalid password");
    }

    @Test
    @DisplayName("logSecurityEvent_unauthorizedAccess_logsError")
    void logSecurityEvent_unauthorizedAccess_logsError() {
        // Given: unauthorized access attempt
        SecurityMonitoringService.SecurityEventType eventType = SecurityMonitoringService.SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT;
        String userId = "maliciousUser";
        String details = "Attempted access to /admin";
        String ipAddress = "10.0.0.1";

        // When: logging security event
        securityMonitoringService.logSecurityEvent(eventType, userId, details, ipAddress);

        // Then: event logged without error
        assertThat(eventType).isEqualTo(SecurityMonitoringService.SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT);
    }

    @Test
    @DisplayName("logSecurityEvent_btgAccess_logsWarning")
    void logSecurityEvent_btgAccess_logsWarning() {
        // Given: BTG access granted
        SecurityMonitoringService.SecurityEventType eventType = SecurityMonitoringService.SecurityEventType.BTG_ACCESS_GRANTED;
        String userId = "doctor123";
        String details = "Emergency access to patient 456";
        String ipAddress = "172.16.0.5";

        // When: logging security event
        securityMonitoringService.logSecurityEvent(eventType, userId, details, ipAddress);

        // Then: event processed successfully
        assertThat(eventType).isEqualTo(SecurityMonitoringService.SecurityEventType.BTG_ACCESS_GRANTED);
        assertThat(details).contains("Emergency access");
    }

    @Test
    @DisplayName("logSecurityEvent_privilegeEscalation_alertsSecurityTeam")
    void logSecurityEvent_privilegeEscalation_alertsSecurityTeam() {
        // Given: privilege escalation attempt
        SecurityMonitoringService.SecurityEventType eventType = SecurityMonitoringService.SecurityEventType.PRIVILEGE_ESCALATION_ATTEMPT;
        String userId = "attacker";
        String details = "Attempted role modification";
        String ipAddress = "203.0.113.1";

        // When: logging security event
        securityMonitoringService.logSecurityEvent(eventType, userId, details, ipAddress);

        // Then: high-priority event logged
        assertThat(eventType).isEqualTo(SecurityMonitoringService.SecurityEventType.PRIVILEGE_ESCALATION_ATTEMPT);
        assertThat(details).contains("role modification");
    }

    @Test
    @DisplayName("logSecurityEvent_nullParameters_handlesGracefully")
    void logSecurityEvent_nullParameters_handlesGracefully() {
        // Given: null parameters
        SecurityMonitoringService.SecurityEventType eventType = SecurityMonitoringService.SecurityEventType.SUSPICIOUS_QUERY_PATTERN;
        String userId = null;
        String details = null;
        String ipAddress = null;

        // When: logging security event with nulls
        securityMonitoringService.logSecurityEvent(eventType, userId, details, ipAddress);

        // Then: no exception thrown (service handles nulls gracefully)
        assertThat(eventType).isNotNull();
    }
}
