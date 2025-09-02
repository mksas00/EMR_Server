package com.example.emr_server.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequestDto {
    @NotBlank
    @Size(min = 3, max = 254)
    private String usernameOrEmail;
}
