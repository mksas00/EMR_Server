package com.example.emr_server.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String username,
        String email,
        String role,
        Boolean accountLocked,
        Boolean mfaEnabled,
        Integer failedLoginAttempts,
        Instant lastLoginAt,
        String lastLoginIp,
        Instant createdAt
) {}

