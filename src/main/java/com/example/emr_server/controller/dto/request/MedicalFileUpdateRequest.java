package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

public record MedicalFileUpdateRequest(
        @NotBlank @Size(max = 10000) String fileName,
        @NotBlank @Size(max = 10000) String filePath,
        @NotBlank @Size(max = 10000) String mimeType
) {}

