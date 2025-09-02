package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BtgGrantRequest(
        @NotNull UUID patientId,
        @Min(1) @Max(120) int minutes,
        @NotBlank @Size(min = 3, max = 500) String reason
) {}
