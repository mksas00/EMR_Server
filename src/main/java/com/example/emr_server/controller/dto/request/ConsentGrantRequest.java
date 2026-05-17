package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Nadaj zgodę dostępu do danych pacjenta innemu użytkownikowi systemu.
 *
 * scope: "read" – odczyt; "write" – odczyt + zapis.
 * expiresAt: opcjonalne – jeśli puste, zgoda trwa bezterminowo do cofnięcia.
 */
public record ConsentGrantRequest(
        @NotNull UUID patientId,
        @NotNull UUID grantedToUserId,
        @NotBlank @Pattern(regexp = "read|write", message = "scope musi być read lub write") String scope,
        @Size(max = 1000) String reason,
        Instant expiresAt
) {}

