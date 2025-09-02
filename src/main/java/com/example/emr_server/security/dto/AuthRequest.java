package com.example.emr_server.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
    @NotBlank
    @Size(min = 3, max = 254)
    private String usernameOrEmail;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @Size(min = 6, max = 32)
    private String mfaCode; // opcjonalnie przy logowaniu gdy MFA włączone

    @Size(max = 512)
    private String challengeToken; // opcjonalny token wyzwania (drugi krok)
}
