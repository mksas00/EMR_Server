package com.example.emr_server.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * Security testing configuration properties
 * Used for configuring security parameters during testing phases
 */
@Component
@ConfigurationProperties(prefix = "app.security.testing")
@Data
public class SecurityTestingProperties {
    
    /**
     * Enable/disable security testing mode
     */
    private boolean enabled = false;
    
    /**
     * Maximum allowed failed authentication attempts during testing
     */
    private int maxFailedAttempts = 3;
    
    /**
     * Rate limiting configuration for testing
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();
    
    /**
     * Security headers enforcement during testing
     */
    private HeadersConfig headers = new HeadersConfig();
    
    @Data
    public static class RateLimitConfig {
        private int requestsPerMinute = 30;
        private int burstCapacity = 10;
        private boolean enableBruteForceProtection = true;
    }
    
    @Data
    public static class HeadersConfig {
        private boolean enforceHttps = true;
        private boolean enableHsts = true;
        private boolean enableCsp = true;
        private boolean enableCors = true;
        private int hstsMaxAge = 31536000; // 1 year
    }
}
