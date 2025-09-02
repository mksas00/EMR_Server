package com.example.emr_server.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = 8, max = 128)
    private String currentPassword;

    @NotBlank
    @Size(min = 12, max = 128)
    private String newPassword;
}
