package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record AllergyCreateRequest(
        @NotNull UUID patientId,
        @NotBlank @Size(max = 10000) String allergen,
        @Size(max = 10000) String reaction,
        @Size(max = 20) String severity
) {}

