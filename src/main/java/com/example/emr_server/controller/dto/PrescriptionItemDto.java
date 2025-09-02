package com.example.emr_server.controller.dto;

import java.util.UUID;

public record PrescriptionItemDto(
        UUID id,
        UUID medicationId,
        String dosageInfo,
        Integer quantity,
        String unit
) {}
