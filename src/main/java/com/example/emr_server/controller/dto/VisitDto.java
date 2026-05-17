package com.example.emr_server.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record VisitDto(
        UUID id,
        UUID patientId,
        String patientFullName,
        UUID doctorId,
        String doctorFullName,
        Instant visitDate,
        Instant endDate,
        String visitType,
        String reason,
        String diagnosis,
        String notes,
        Boolean confidential,
        String status
) {}
