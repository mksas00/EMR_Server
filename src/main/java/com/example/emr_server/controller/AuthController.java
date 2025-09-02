package com.example.emr_server.controller;

import com.example.emr_server.security.AuthenticationService;
import com.example.emr_server.security.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        return ResponseEntity.ok(authenticationService.login(req, ip, ua));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        return ResponseEntity.ok(authenticationService.refresh(req, ip, ua));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest req) {
        authenticationService.logout(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions() {
        return ResponseEntity.ok(authenticationService.listOwnSessions());
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID id) {
        authenticationService.revokeSession(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authenticationService.changePassword(req);
        return ResponseEntity.noContent().build();
    }

    // ===== MFA endpoints =====
    @PostMapping("/mfa/start")
    public ResponseEntity<MfaSetupResponse> startMfa() {
        return ResponseEntity.ok(authenticationService.startMfaSetup());
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<MfaConfirmResponse> confirmMfa(@Valid @RequestBody MfaConfirmRequest req) {
        return ResponseEntity.ok(authenticationService.confirmMfaSetup(req));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa() {
        authenticationService.disableMfa();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mfa/status")
    public ResponseEntity<MfaStatusResponse> mfaStatus() {
        return ResponseEntity.ok(authenticationService.mfaStatus());
    }

    @PostMapping("/mfa/recovery/regenerate")
    public ResponseEntity<MfaRecoveryCodesResponse> regenerateRecovery() {
        return ResponseEntity.ok(authenticationService.regenerateRecoveryCodes());
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> passwordResetRequest(@Valid @RequestBody PasswordResetRequestDto dto, HttpServletRequest request) {
        authenticationService.requestPasswordReset(dto, request.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> passwordResetConfirm(@Valid @RequestBody PasswordResetConfirmDto dto, HttpServletRequest request) {
        authenticationService.confirmPasswordReset(dto, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
