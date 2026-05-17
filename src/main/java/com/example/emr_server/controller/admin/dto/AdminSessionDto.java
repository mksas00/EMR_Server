package com.example.emr_server.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminSessionDto(
        UUID id,
        UUID userId,
        String username,
        String role,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        String ip,
        String userAgent,
        boolean active
) {}

