package com.example.emr_server.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        Integer id,
        Instant timestamp,
        String action,
        String description,
        UUID userId,
        String username,
        UUID patientId
) {}

