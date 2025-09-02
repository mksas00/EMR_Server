package com.example.emr_server.controller.dto;

import java.util.UUID;

public record AllergyDto(
        UUID id,
        UUID patientId,
        UUID notedById,
        String allergen,
        String reaction,
        String severity
) {}

