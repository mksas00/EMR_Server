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
public class MfaRecoveryCodesResponse {
    private List<String> recoveryCodes; // plaintext nowych kodów (pokazywane tylko przy regeneracji)
}

