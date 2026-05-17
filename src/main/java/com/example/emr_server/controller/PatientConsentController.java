package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.PatientConsentDto;
import com.example.emr_server.controller.dto.request.ConsentGrantRequest;
import com.example.emr_server.service.PatientConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Zarządzanie zgodami dostępu do danych pacjenta (RODO / GDPR).
 *
 * Endpoints:
 *  POST   /api/consents            – nadaj zgodę
 *  DELETE /api/consents/{id}       – cofnij zgodę
 *  GET    /api/consents/patient/{patientId} – lista zgód pacjenta
 *  GET    /api/consents/mine       – moje aktywne zgody (jakie dostępy mam)
 */
@RestController
@RequestMapping("/api/consents")
@Tag(name = "Zgody dostępu", description = "Zarządzanie zgodami dostępu do danych pacjenta (RODO)")
public class PatientConsentController {

    private final PatientConsentService consentService;

    public PatientConsentController(PatientConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Nadaj zgodę dostępu", description = "Lekarz/admin nadaje innemu użytkownikowi dostęp (read/write) do pacjenta")
    public ResponseEntity<PatientConsentDto> grant(@Valid @RequestBody ConsentGrantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consentService.grant(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Cofnij zgodę")
    public ResponseEntity<PatientConsentDto> revoke(@PathVariable UUID id) {
        return ResponseEntity.ok(consentService.revoke(id));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','LAB_TECH','ADMIN')")
    @Operation(summary = "Zgody dla pacjenta", description = "Lista wszystkich zgód dostępu do wskazanego pacjenta")
    public ResponseEntity<Page<PatientConsentDto>> listForPatient(
            @PathVariable UUID patientId,
            Pageable pageable) {
        return ResponseEntity.ok(consentService.listForPatient(patientId, pageable));
    }

    @GetMapping("/mine")
    @Operation(summary = "Moje aktywne zgody", description = "Zgody dostępu do pacjentów, które mi nadano")
    public ResponseEntity<Page<PatientConsentDto>> listMine(Pageable pageable) {
        return ResponseEntity.ok(consentService.listMyActiveConsents(pageable));
    }
}

