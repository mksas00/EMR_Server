package com.example.emr_server.security;

import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PatientConsentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthorizationService {

    private final PatientConsentRepository consentRepository;

    private static final Set<String> ADMIN_OR_SYSTEM = Set.of("admin");
    private static final Set<String> CLINICAL_ROLES = Set.of("doctor","nurse","lab_tech");

    public AuthorizationService(PatientConsentRepository consentRepository) {
        this.consentRepository = consentRepository;
    }

    public boolean canReadPatient(User user, Patient patient) {
        if (user == null) return false;
        String role = user.getRole();
        if (role != null) role = role.toLowerCase(Locale.ROOT);
        if (ADMIN_OR_SYSTEM.contains(role)) return true;
        if (CLINICAL_ROLES.contains(role)) {
            if (isCreator(user, patient)) return true;
            if (hasActiveConsent(user, patient, "read")) return true;
        }
        return false;
    }

    public boolean canWritePatient(User user, Patient patient) {
        if (user == null) return false;
        String role = user.getRole();
        if (role != null) role = role.toLowerCase(Locale.ROOT);
        if (ADMIN_OR_SYSTEM.contains(role)) return true;
        if (CLINICAL_ROLES.contains(role)) {
            if (isCreator(user, patient)) return true;
            if (hasActiveConsent(user, patient, "write")) return true;
        }
        return false;
    }

    public boolean canDeletePatient(User user, Patient patient) {
        if (user == null) return false;
        String role = user.getRole();
        if (role != null) role = role.toLowerCase(Locale.ROOT);
        if (ADMIN_OR_SYSTEM.contains(role)) return true;
        // bardziej restrykcyjne: tylko admin; ewentualnie lekarz-twórca + consent typu admin_delete
        return false;
    }

    private boolean isCreator(User user, Patient patient) {
        try {
            return patient.getCreatedBy() != null && patient.getCreatedBy().getId().equals(user.getId());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasActiveConsent(User user, Patient patient, String scope) {
        return consentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, user, scope)
                .filter(c -> c.getRevokedAt() == null || c.getRevokedAt().isAfter(Instant.now()))
                .isPresent();
    }

    public void assertCanRead(User user, Patient patient) {
        if (!canReadPatient(user, patient)) throw new SecurityException("Brak uprawnień do odczytu pacjenta");
    }
    public void assertCanWrite(User user, Patient patient) {
        if (!canWritePatient(user, patient)) throw new SecurityException("Brak uprawnień do modyfikacji pacjenta");
    }
    public void assertCanDelete(User user, Patient patient) {
        if (!canDeletePatient(user, patient)) throw new SecurityException("Brak uprawnień do usunięcia pacjenta");
    }
}
