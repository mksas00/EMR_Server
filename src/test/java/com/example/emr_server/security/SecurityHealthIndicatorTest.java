package com.example.emr_server.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityHealthIndicator
 * Tests security health monitoring functionality
 */
@ExtendWith(MockitoExtension.class)
class SecurityHealthIndicatorTest {

    @InjectMocks
    private SecurityHealthIndicator securityHealthIndicator;

    @Test
    @DisplayName("health_allSecurityComponentsActive_returnsHealthyStatus")
    void health_allSecurityComponentsActive_returnsHealthyStatus() {
        // Given: all security components are working
        // When: checking health status
        Health result = securityHealthIndicator.health();

        // Then: health status is UP with security details
        assertThat(result.getStatus()).isEqualTo(Status.UP);
        assertThat(result.getDetails()).isNotEmpty();
        assertThat(result.getDetails()).containsKey("https");
        assertThat(result.getDetails()).containsKey("rateLimit");
        assertThat(result.getDetails()).containsKey("auditing");
        assertThat(result.getDetails()).containsKey("encryption");
        assertThat(result.getDetails()).containsKey("securityScore");
    }

    @Test
    @DisplayName("health_securityComponentsCheck_containsExpectedDetails")
    void health_securityComponentsCheck_containsExpectedDetails() {
        // Given: security health indicator
        // When: performing health check
        Health result = securityHealthIndicator.health();

        // Then: health details contain security component status
        assertThat(result.getDetails().get("https")).isEqualTo("enforced");
        assertThat(result.getDetails().get("rateLimit")).isEqualTo("active");
        assertThat(result.getDetails().get("auditing")).isEqualTo("active");
        assertThat(result.getDetails().get("encryption")).isEqualTo("active");
        assertThat(result.getDetails().get("securityScore")).isEqualTo("100%");
    }

    @Test
    @DisplayName("health_noExceptions_completesSuccessfully")
    void health_noExceptions_completesSuccessfully() {
        // Given: security health indicator
        // When: checking health multiple times
        Health result1 = securityHealthIndicator.health();
        Health result2 = securityHealthIndicator.health();

        // Then: both calls succeed without exceptions
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result1.getStatus()).isEqualTo(result2.getStatus());
    }
}
