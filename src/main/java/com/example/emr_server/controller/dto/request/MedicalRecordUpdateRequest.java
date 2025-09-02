package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.util.Map;

public record MedicalRecordUpdateRequest(
        @NotBlank @Size(max = 50) String recordType,
        Map<String, Object> content,
        Boolean isEncrypted,
        @Size(max = 128) String encryptedChecksum
) {}

