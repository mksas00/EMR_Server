package com.example.emr_server.controller.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MedicalRecordDto(
        UUID id,
        UUID patientId,
        UUID createdById,
        String recordType,
        Map<String, Object> content,
        Boolean isEncrypted,
        String encryptedChecksum,
        Instant createdAt
) {}

