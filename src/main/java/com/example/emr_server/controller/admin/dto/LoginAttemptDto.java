package com.example.emr_server.controller.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginAttemptDto(
        UUID id,
        Instant timestamp,
        Boolean success,
        String ip,
        String userAgent,
        UUID userId,
        String username
) {}

