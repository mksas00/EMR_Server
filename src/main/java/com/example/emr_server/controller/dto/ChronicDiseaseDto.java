package com.example.emr_server.controller.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ChronicDiseaseDto(
        UUID id,
        UUID patientId,
        String diseaseName,
        LocalDate diagnosedDate,
        String notes
) {}

