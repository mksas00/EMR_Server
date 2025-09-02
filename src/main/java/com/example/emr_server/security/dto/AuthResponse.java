package com.example.emr_server.security.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType = "Bearer";
    private final long expiresInSeconds;
    private final Boolean mfaRequired; // null/false gdy nie wymagane
    private final String challengeToken; // JWT wyzwania MFA (krótko żyjący)
}
