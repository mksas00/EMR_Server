package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record MedicalFileCreateRequest(
        @NotNull UUID patientId,
        @NotBlank @Size(max = 10000) String fileName,
        @NotBlank @Size(max = 10000) String filePath,
        @NotBlank @Size(max = 10000) String mimeType
) {}

