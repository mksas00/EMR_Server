package com.example.emr_server.controller.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LabResultDto(
        UUID id,
        UUID patientId,
        UUID orderedById,
        String testName,
        String result,
        LocalDate resultDate,
        String unit,
        String referenceRange,
        String status
) {}

