package com.example.emr_server.security;

import com.example.emr_server.entity.SecurityIncident;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.SecurityIncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Centralny punkt rejestrowania incydentów bezpieczeństwa.
 *
 * <p>Wszystkie zapisy idą w niezależnej transakcji ({@link Propagation#REQUIRES_NEW}),
 * dzięki czemu rollback transakcji wywołującej (np. po rzuceniu wyjątku
 * autoryzacyjnego) nie kasuje śladu po incydencie.</p>
 *
 * <p>Severity:
 * <ul>
 *   <li><b>low</b> – sygnał diagnostyczny, brak realnego ryzyka</li>
 *   <li><b>medium</b> – nadużycie / podejrzane zachowanie</li>
 *   <li><b>high</b> – potwierdzone naruszenie polityki, wymaga przeglądu</li>
 *   <li><b>critical</b> – aktywny atak / wyciek, alarmowy charakter</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityIncidentService {

    private final SecurityIncidentRepository incidentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityIncident raise(String severity, String category, String description, User user) {
        try {
            SecurityIncident si = new SecurityIncident();
            si.setId(UUID.randomUUID());
            si.setDetectedAt(Instant.now());
            si.setSeverity(severity);
            si.setCategory(category);
            si.setDescription(description);
            si.setStatus("open");
            si.setUser(user);
            SecurityIncident saved = incidentRepository.save(si);
            log.warn("SECURITY_INCIDENT severity={} category={} user={} desc={}",
                    severity, category,
                    user != null ? user.getUsername() : "n/a",
                    description);
            return saved;
        } catch (Exception ex) {
            // nie wolno wywrócić wywołującego – tylko logujemy
            log.error("Nie udało się zapisać incydentu {} {}: {}", severity, category, ex.getMessage());
            return null;
        }
    }

    public void low(String category, String description, User user) {
        raise("low", category, description, user);
    }

    public void medium(String category, String description, User user) {
        raise("medium", category, description, user);
    }

    public void high(String category, String description, User user) {
        raise("high", category, description, user);
    }

    public void critical(String category, String description, User user) {
        raise("critical", category, description, user);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Predefined categories – stała lista, żeby nie literować
    // ─────────────────────────────────────────────────────────────────
    public static final class Cat {
        public static final String ACCOUNT_LOCK = "ACCOUNT_LOCK";
        public static final String REFRESH_ON_LOCKED = "REFRESH_ON_LOCKED";
        public static final String RATE_LIMIT = "RATE_LIMIT";
        public static final String AUTHORIZATION = "AUTHORIZATION";
        public static final String UNAUTHORIZED_PHI_ACCESS = "UNAUTHORIZED_PHI_ACCESS";
        public static final String BRUTE_FORCE_BURST = "BRUTE_FORCE_BURST";
        public static final String CREDENTIAL_STUFFING = "CREDENTIAL_STUFFING";
        public static final String MFA_BYPASS_ATTEMPT = "MFA_BYPASS_ATTEMPT";
        public static final String PASSWORD_RESET_ABUSE = "PASSWORD_RESET_ABUSE";
        public static final String WEAK_PASSWORD_REJECTED = "WEAK_PASSWORD_REJECTED";
        public static final String EXPIRED_TOKEN_USAGE = "EXPIRED_TOKEN_USAGE";
        public static final String SQL_INJECTION_DETECTED = "SQL_INJECTION_DETECTED";
        public static final String XSS_DETECTED = "XSS_DETECTED";
        public static final String BTG_ABUSE = "BTG_ABUSE";
        public static final String PRIVILEGE_ESCALATION = "PRIVILEGE_ESCALATION";
        public static final String SESSION_HIJACK_SUSPECTED = "SESSION_HIJACK_SUSPECTED";
        public static final String MASS_DATA_EXPORT = "MASS_DATA_EXPORT";

        private Cat() {}
    }
}

