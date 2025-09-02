package com.example.emr_server.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaConfirmResponse {
    private boolean enabled;              // czy MFA włączone
    private List<String> recoveryCodes;   // plaintext kodów (tylko raz przy confirm)
}

