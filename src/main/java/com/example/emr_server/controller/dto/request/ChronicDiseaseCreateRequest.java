package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record ChronicDiseaseCreateRequest(
        @NotNull UUID patientId,
        @NotBlank @Size(max = 10000) String diseaseName,
        LocalDate diagnosedDate,
        @Size(max = 10000) String notes
) {}

