package com.example.emr_server.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmDto {
    @NotBlank
    @Size(min = 20, max = 512)
    private String token;       // surowy token otrzymany (np. z maila/logu)

    @NotBlank
    @Size(min = 12, max = 128)
    private String newPassword; // nowe hasło
}
