package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record PrescriptionUpdateRequest(
        @Size(max = 10000) String dosageInfo,
        LocalDate issuedDate,
        LocalDate expirationDate,
        Boolean isRepeatable,
        List<PrescriptionItemRequest> items
) {}
