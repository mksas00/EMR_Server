package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PrescriptionCreateRequest(
        @NotNull UUID patientId,
        @Size(max = 10000) String dosageInfo,
        LocalDate issuedDate,
        LocalDate expirationDate,
        Boolean isRepeatable,
        List<PrescriptionItemRequest> items
) {}
