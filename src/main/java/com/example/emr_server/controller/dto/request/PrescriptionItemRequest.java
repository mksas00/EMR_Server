package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PrescriptionItemRequest(
        UUID medicationId,
        @Size(max = 10000) String dosageInfo,
        Integer quantity,
        @Size(max = 50) String unit
) {}
