package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.util.Map;
import java.util.UUID;

public record MedicalRecordCreateRequest(
        @NotNull UUID patientId,
        @NotBlank @Size(max = 50) String recordType,
        Map<String, Object> content,
        Boolean isEncrypted,
        @Size(max = 128) String encryptedChecksum
) {}

