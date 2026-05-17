package com.example.emr_server.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record SecurityIncidentDto(
        UUID id,
        Instant detectedAt,
        String severity,
        String category,
        String description,
        String status,
        UUID userId,
        String username
) {}

