package com.example.emr_server.service;

import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PatientConsentRepository;
import com.example.emr_server.repository.PatientRepository;
import com.example.emr_server.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class BtgServiceImpl implements BtgService {

    private final PatientRepository patientRepository;
    private final PatientConsentRepository patientConsentRepository;

    public BtgServiceImpl(PatientRepository patientRepository, PatientConsentRepository patientConsentRepository) {
        this.patientRepository = patientRepository;
        this.patientConsentRepository = patientConsentRepository;
    }

    @Override
    @Transactional
    public PatientConsent grantBtgConsent(UUID patientId, int minutes, String reason) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono pacjenta"));

        User currentUser = getCurrentUser()
                .orElseThrow(() -> new SecurityException("Brak użytkownika w kontekście"));

        Optional<PatientConsent> existing = patientConsentRepository
                .findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        if (existing.isPresent()) {
            PatientConsent ec = existing.get();
            if (ec.getExpiresAt() != null && ec.getExpiresAt().isAfter(Instant.now())) {
                return ec;
            }
        }

        PatientConsent consent = new PatientConsent();
        consent.setId(UUID.randomUUID());
        consent.setPatient(patient);
        consent.setGrantedTo(currentUser);
        consent.setScope("btg");
        consent.setGrantedAt(Instant.now());
        consent.setReason(reason);
        consent.setExpiresAt(Instant.now().plus(minutes, ChronoUnit.MINUTES));

        return patientConsentRepository.save(consent);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientConsent> getActiveBtgConsent(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono pacjenta"));
        User currentUser = getCurrentUser()
                .orElseThrow(() -> new SecurityException("Brak użytkownika w kontekście"));
        return patientConsentRepository
                .findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg")
                .filter(pc -> pc.getExpiresAt() != null && pc.getExpiresAt().isAfter(Instant.now()));
    }

    @Override
    @Transactional
    public boolean revokeBtgConsent(UUID patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono pacjenta"));
        User currentUser = getCurrentUser()
                .orElseThrow(() -> new SecurityException("Brak użytkownika w kontekście"));
        Optional<PatientConsent> existing = patientConsentRepository
                .findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg")
                .filter(pc -> pc.getExpiresAt() == null || pc.getExpiresAt().isAfter(Instant.now()));
        if (existing.isPresent()) {
            PatientConsent pc = existing.get();
            pc.setRevokedAt(Instant.now());
            patientConsentRepository.save(pc);
            return true;
        }
        return false;
    }

    private Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails cud) {
            return Optional.ofNullable(cud.getDomainUser());
        }
        return Optional.empty();
    }
}
