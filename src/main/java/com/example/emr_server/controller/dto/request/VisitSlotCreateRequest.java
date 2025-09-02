package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record VisitSlotCreateRequest(
        UUID doctorId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        String status
) {}

