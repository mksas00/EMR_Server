package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record VisitUpdateRequest(
        @NotNull Instant visitDate,
        Instant endDate,
        @Size(max = 50) String visitType,
        @Size(max = 10000) String reason,
        @Size(max = 10000) String diagnosis,
        @Size(max = 10000) String notes,
        Boolean isConfidential,
        String status
) {}
