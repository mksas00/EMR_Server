package com.example.emr_server.controller.dto;

import java.util.UUID;

public record MedicationPackageDto(
        UUID id,
        UUID medicationId,
        String gtin,
        String packDescription,
        String supplyStatus
) {}

