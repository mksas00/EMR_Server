package com.example.emr_server.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record VisitSlotDto(
        UUID id,
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        String status,
        UUID patientId,
        UUID visitId
) {}

