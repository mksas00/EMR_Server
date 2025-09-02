package com.example.emr_server.service.dto;

import jakarta.validation.constraints.NotBlank;

public record MedicationUrplImportRequest(
        Long urplId,
        @NotBlank String medicinalProductName,
        String commonName,
        String pharmaceuticalFormName,
        String medicinalProductPower,
        String activeSubstanceName,
        String subjectMedicinalProductName,
        String registryNumber,
        String procedureTypeName,
        String expirationDateString, // "Bezterminowe" lub YYYY-MM-DD
        String atcCode,
        String targetSpecies,
        String prescriptionCategory,
        String packagingConsent,
        String gtin,
        String packDescription
) {}

