package com.example.emr_server.controller.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MedicationDto(
        UUID id,
        String name,
        String commonName,
        String strength,
        String pharmaceuticalForm,
        String authorizationNumber,
        LocalDate authorizationValidTo,
        String marketingAuthorizationHolder,
        String procedureType,
        String legalBasis,
        String atcCode,
        String activeSubstances,
        String targetSpecies,
        String prescriptionCategory
) {}

