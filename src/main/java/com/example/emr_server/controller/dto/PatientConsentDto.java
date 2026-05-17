package com.example.emr_server.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record PatientConsentDto(
        UUID id,
        UUID patientId,
        String patientFullName,
        UUID grantedToId,
        String grantedToUsername,
        String grantedToRole,
        String scope,
        Instant grantedAt,
        Instant expiresAt,
        Instant revokedAt,
        String reason,
        boolean active
) {}

