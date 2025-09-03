package com.example.emr_server.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for security components
 * Provides mock/simplified beans for testing
 */
@TestConfiguration
@Profile("test")
public class SecurityTestConfiguration {

    @Bean
    @Primary
    public SecurityMonitoringService testSecurityMonitoringService() {
        return new SecurityMonitoringService() {
            @Override
            public void logSecurityEvent(SecurityEventType eventType, String userId, String details, String ipAddress) {
                // Simplified logging for tests - just log to console
                System.out.println("TEST SECURITY EVENT: " + eventType + " - " + details);
            }
        };
    }

    @Bean
    @Primary
    public InputValidationService testInputValidationService() {
        return new InputValidationService() {
            @Override
            public boolean isSafeInput(String input) {
                // Simplified validation for tests
                return input == null || !input.contains("<script>");
            }
        };
    }
}
