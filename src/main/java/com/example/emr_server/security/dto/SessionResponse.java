package com.example.emr_server.security.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SessionResponse {
    private final UUID id;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final Instant revokedAt;
    private final String ip;
    private final String userAgent;
    private final boolean active;
}

