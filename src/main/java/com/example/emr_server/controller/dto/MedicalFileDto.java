package com.example.emr_server.controller.dto;

import java.time.Instant;
import java.util.UUID;

public record MedicalFileDto(
        UUID id,
        UUID patientId,
        UUID uploadedById,
        String fileName,
        String mimeType,
        Instant uploadedAt
) {}

