package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record LabResultUpdateRequest(
        @NotBlank @Size(max = 10000) String testName,
        @Size(max = 10000) String result,
        LocalDate resultDate,
        @Size(max = 20) String unit,
        @Size(max = 10000) String referenceRange,
        @Size(max = 20) String status
) {}

