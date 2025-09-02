package com.example.emr_server.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.auth")
public class SecurityAuthProperties {
    private int maxFailed = 5;
    private int windowMinutes = 15;

    public int getMaxFailed() { return maxFailed; }
    public void setMaxFailed(int maxFailed) { this.maxFailed = maxFailed; }
    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }
}

