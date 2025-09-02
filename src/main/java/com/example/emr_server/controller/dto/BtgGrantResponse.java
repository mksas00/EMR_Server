package com.example.emr_server.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record BtgGrantResponse(
        UUID consentId,
        Instant expiresAt
) {}

