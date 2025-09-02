package com.example.emr_server.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MfaConfirmRequest {
    @NotBlank
    @Size(min = 6, max = 10)
    private String code; // kod TOTP lub recovery przy potwierdzaniu konfiguracji
}
