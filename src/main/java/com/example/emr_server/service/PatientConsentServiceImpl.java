package com.example.emr_server.service;

import com.example.emr_server.controller.dto.PatientConsentDto;
import com.example.emr_server.controller.dto.request.ConsentGrantRequest;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PatientConsentRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class PatientConsentServiceImpl implements PatientConsentService {

    private final PatientConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final PatientService patientService;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public PatientConsentServiceImpl(PatientConsentRepository consentRepository,
                                     UserRepository userRepository,
                                     PatientService patientService,
                                     AuthorizationService authorizationService,
                                     AuditService auditService) {
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.patientService = patientService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    private User current() {
        return SecurityUtil.getCurrentUser(userRepository)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    private PatientConsentDto toDto(PatientConsent c) {
        Patient p = c.getPatient();
        User g = c.getGrantedTo();
        boolean active = c.getRevokedAt() == null
                && (c.getExpiresAt() == null || c.getExpiresAt().isAfter(Instant.now()));
        return new PatientConsentDto(
                c.getId(),
                p != null ? p.getId() : null,
                p != null ? (safe(p.getFirstName()) + " " + safe(p.getLastName())).trim() : null,
                g != null ? g.getId() : null,
                g != null ? g.getUsername() : null,
                g != null ? g.getRole() : null,
                c.getScope(),
                c.getGrantedAt(),
                c.getExpiresAt(),
                c.getRevokedAt(),
                c.getReason(),
                active
        );
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ─── grant ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PatientConsentDto grant(ConsentGrantRequest req) {
        User granter = current();

        Patient patient = patientService.findPatientById(req.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacjent nie istnieje"));

        // Granter musi mieć prawo zapisu do pacjenta (lub być adminem)
        if (!authorizationService.canWritePatient(granter, patient)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nie masz uprawnień do nadawania zgód dla tego pacjenta");
        }

        User grantedTo = userRepository.findById(req.grantedToUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Użytkownik nie istnieje"));

        // Nie można nadać sobie zgody
        if (granter.getId().equals(grantedTo.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nie można nadać zgody samemu sobie");
        }

        // Walidacja expiresAt
        if (req.expiresAt() != null && req.expiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data wygaśnięcia nie może być w przeszłości");
        }

        // Unikalna aktywna zgoda – jeśli istnieje, cofamy i wystawiamy nową (idempotentne)
        consentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, grantedTo, req.scope())
                .ifPresent(existing -> {
                    existing.setRevokedAt(Instant.now());
                    consentRepository.save(existing);
                });

        PatientConsent consent = new PatientConsent();
        consent.setId(UUID.randomUUID());
        consent.setPatient(patient);
        consent.setGrantedTo(grantedTo);
        consent.setScope(req.scope());
        consent.setGrantedAt(Instant.now());
        consent.setReason(req.reason());
        consent.setExpiresAt(req.expiresAt());

        PatientConsent saved = consentRepository.save(consent);

        auditService.logPatient(granter, patient, "GRANT_CONSENT",
                String.format("grantedTo=%s,scope=%s,expiresAt=%s",
                        grantedTo.getUsername(), req.scope(), req.expiresAt()));

        return toDto(saved);
    }

    // ─── revoke ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PatientConsentDto revoke(UUID consentId) {
        User actor = current();

        PatientConsent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zgoda nie istnieje"));

        if (consent.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zgoda już cofnięta");
        }

        // Cofać może: admin, lub osoba z dostępem write do pacjenta
        boolean isAdmin = "admin".equals(actor.getRole());
        if (!isAdmin && !authorizationService.canWritePatient(actor, consent.getPatient())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do cofnięcia zgody");
        }

        consent.setRevokedAt(Instant.now());
        PatientConsent saved = consentRepository.save(consent);

        auditService.logPatient(actor, consent.getPatient(), "REVOKE_CONSENT",
                String.format("consentId=%s,grantedTo=%s,scope=%s",
                        consentId,
                        consent.getGrantedTo() != null ? consent.getGrantedTo().getUsername() : "?",
                        consent.getScope()));

        return toDto(saved);
    }

    // ─── queries ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PatientConsentDto> listForPatient(UUID patientId, Pageable pageable) {
        User actor = current();
        Patient patient = patientService.findPatientById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacjent nie istnieje"));
        if (!authorizationService.canReadPatient(actor, patient)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak dostępu do pacjenta");
        }
        return consentRepository
                .findByPatient_IdOrderByGrantedAtDesc(patientId, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientConsentDto> listMyActiveConsents(Pageable pageable) {
        User actor = current();
        return consentRepository
                .findActiveConsentsByGrantedTo(actor.getId(), Instant.now(), pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientConsentDto> listGrantedByMe(UUID patientId, Pageable pageable) {
        User actor = current();
        Patient patient = patientService.findPatientById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacjent nie istnieje"));
        if (!authorizationService.canReadPatient(actor, patient)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak dostępu do pacjenta");
        }
        // Filtrujemy z listy wszystkich zgód dla pacjenta
        return consentRepository
                .findByPatient_IdOrderByGrantedAtDesc(patientId, pageable)
                .map(this::toDto);
    }
}

