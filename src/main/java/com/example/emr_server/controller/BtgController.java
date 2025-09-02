package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.BtgGrantResponse;
import com.example.emr_server.controller.dto.request.BtgGrantRequest;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.service.BtgService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/btg")
public class BtgController {

    private final BtgService btgService;

    public BtgController(BtgService btgService) {
        this.btgService = btgService;
    }

    @PostMapping("/grant")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE')")
    public ResponseEntity<BtgGrantResponse> grant(@Valid @RequestBody BtgGrantRequest req) {
        PatientConsent consent = btgService.grantBtgConsent(req.patientId(), req.minutes(), req.reason());
        return ResponseEntity.ok(new BtgGrantResponse(consent.getId(), consent.getExpiresAt()));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE')")
    public ResponseEntity<BtgStatusResponse> status(@RequestParam UUID patientId) {
        Optional<PatientConsent> opt = btgService.getActiveBtgConsent(patientId);
        return ResponseEntity.ok(new BtgStatusResponse(opt.isPresent(), opt.map(PatientConsent::getExpiresAt).orElse(null)));
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE')")
    public ResponseEntity<BtgRevokeResponse> revoke(@Valid @RequestBody BtgRevokeRequest req) {
        boolean revoked = btgService.revokeBtgConsent(req.patientId());
        return ResponseEntity.ok(new BtgRevokeResponse(revoked));
    }

    public record BtgStatusResponse(boolean active, java.time.Instant expiresAt) {}
    public record BtgRevokeRequest(@jakarta.validation.constraints.NotNull UUID patientId) {}
    public record BtgRevokeResponse(boolean revoked) {}
}
