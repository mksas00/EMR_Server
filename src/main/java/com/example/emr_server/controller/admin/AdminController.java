package com.example.emr_server.controller.admin;

import com.example.emr_server.controller.admin.dto.*;
import com.example.emr_server.entity.*;
import com.example.emr_server.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Endpointy administracyjne (bezpieczeństwo, audyt, zarządzanie kontami).
 * Wymagana rola: ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Panel administracyjny – audyt, incydenty, sesje, konta")
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final SecurityIncidentRepository incidentRepository;
    private final UserLoginAttemptRepository loginAttemptRepository;
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public AdminController(AuditLogRepository auditLogRepository,
                           SecurityIncidentRepository incidentRepository,
                           UserLoginAttemptRepository loginAttemptRepository,
                           UserSessionRepository sessionRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.incidentRepository = incidentRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  DASHBOARD
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard bezpieczeństwa", description = "Statystyki: incydenty, sesje, logowania")
    public ResponseEntity<SecurityDashboardDto> dashboard(
            @RequestParam(defaultValue = "7") int windowDays) {
        Instant now = Instant.now();
        Instant since24h = now.minus(24, ChronoUnit.HOURS);
        int wd = Math.max(1, Math.min(windowDays, 365));
        Instant sinceWindow = now.minus(wd, ChronoUnit.DAYS);

        long activeSessions = sessionRepository.countByRevokedAtIsNullAndExpiresAtAfter(now);
        long openIncidents = incidentRepository.countByStatus("open");
        long failedLogins = loginAttemptRepository.countBySuccessAndTimestampAfter(false, since24h);
        long successLogins = loginAttemptRepository.countBySuccessAndTimestampAfter(true, since24h);
        long lockedAccounts = userRepository.countByAccountLockedTrue();
        long totalUsers = userRepository.count();

        SecurityDashboardDto.Stats stats = new SecurityDashboardDto.Stats(
                activeSessions, openIncidents, failedLogins, successLogins, lockedAccounts, totalUsers);

        // Severity breakdown (last 30 days)
        Instant since30d = now.minus(30, ChronoUnit.DAYS);
        List<SecurityDashboardDto.SeverityCount> bySeverity = new ArrayList<>();
        for (String sev : List.of("low", "medium", "high", "critical")) {
            long c = incidentRepository.countBySeverityAndDetectedAtAfter(sev, since30d);
            bySeverity.add(new SecurityDashboardDto.SeverityCount(sev, c));
        }

        // Top failing IPs (configurable window, top 10)
        Instant since7d = sinceWindow;
        List<SecurityDashboardDto.TopItem> topIps = loginAttemptRepository
                .topFailingIpsSince(since7d, PageRequest.of(0, 10))
                .stream()
                .map(row -> new SecurityDashboardDto.TopItem(
                        String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        // Top active users (configurable window, top 10)
        List<SecurityDashboardDto.TopItem> topUsers = loginAttemptRepository
                .topActiveUsersSince(since7d, PageRequest.of(0, 10))
                .stream()
                .map(row -> new SecurityDashboardDto.TopItem(
                        String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();

        // Login timeline – placeholder: ostatnie 24 punkty po 1h
        List<SecurityDashboardDto.TimeBucket> timeline = new ArrayList<>();
        // Brak natywnego time-bucketu w JPA – pomijamy lub odrębne zapytanie SQL w przyszłości.

        return ResponseEntity.ok(new SecurityDashboardDto(stats, bySeverity, timeline, topIps, topUsers));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  AUDIT LOG
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/audit-log")
    @Operation(summary = "Lista wpisów audytu", description = "Filtrowanie po użytkowniku, akcji i czasie")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AuditLogDto>> listAuditLog(
            @RequestParam Optional<String> action,
            @RequestParam Optional<UUID> userId,
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<String> q,
            @RequestParam Optional<Instant> from,
            @RequestParam Optional<Instant> to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Specification<AuditLog> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            action.ifPresent(a -> ps.add(cb.equal(root.get("action"), a)));
            userId.ifPresent(uid -> ps.add(cb.equal(root.get("user").get("id"), uid)));
            patientId.ifPresent(pid -> ps.add(cb.equal(root.get("patient").get("id"), pid)));
            q.ifPresent(s -> ps.add(cb.like(cb.lower(root.get("description")), "%" + s.toLowerCase() + "%")));
            from.ifPresent(f -> ps.add(cb.greaterThanOrEqualTo(root.get("timestamp"), f)));
            to.ifPresent(t -> ps.add(cb.lessThanOrEqualTo(root.get("timestamp"), t)));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogDto> result = auditLogRepository.findAll(spec, pageable).map(this::toDto);
        return ResponseEntity.ok(result);
    }

    private AuditLogDto toDto(AuditLog a) {
        return new AuditLogDto(
                a.getId(), a.getTimestamp(), a.getAction(), a.getDescription(),
                a.getUser() != null ? a.getUser().getId() : null,
                a.getUser() != null ? a.getUser().getUsername() : null,
                a.getPatient() != null ? a.getPatient().getId() : null);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SECURITY INCIDENTS
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/incidents")
    @Operation(summary = "Lista incydentów bezpieczeństwa")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<SecurityIncidentDto>> listIncidents(
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> severity,
            @RequestParam Optional<String> category,
            @RequestParam Optional<UUID> userId,
            @RequestParam Optional<Instant> from,
            @RequestParam Optional<Instant> to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Specification<SecurityIncident> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            status.ifPresent(v -> ps.add(cb.equal(root.get("status"), v)));
            severity.ifPresent(v -> ps.add(cb.equal(root.get("severity"), v)));
            category.ifPresent(v -> ps.add(cb.equal(root.get("category"), v)));
            userId.ifPresent(uid -> ps.add(cb.equal(root.get("user").get("id"), uid)));
            from.ifPresent(f -> ps.add(cb.greaterThanOrEqualTo(root.get("detectedAt"), f)));
            to.ifPresent(t -> ps.add(cb.lessThanOrEqualTo(root.get("detectedAt"), t)));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "detectedAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<SecurityIncidentDto> result = incidentRepository.findAll(spec, pageable).map(this::toDto);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/incidents/{id}/status")
    @Operation(summary = "Zmiana statusu incydentu")
    @Transactional
    public ResponseEntity<SecurityIncidentDto> updateIncidentStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || !List.of("open", "investigating", "resolved", "false_positive").contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Niepoprawny status");
        }
        SecurityIncident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incydent nie istnieje"));
        incident.setStatus(newStatus);
        incidentRepository.save(incident);
        // Build DTO inside the active transaction to avoid LazyInitializationException on user proxy
        return ResponseEntity.ok(toDto(incident));
    }

    private SecurityIncidentDto toDto(SecurityIncident i) {
        return new SecurityIncidentDto(
                i.getId(), i.getDetectedAt(), i.getSeverity(), i.getCategory(),
                i.getDescription(), i.getStatus(),
                i.getUser() != null ? i.getUser().getId() : null,
                i.getUser() != null ? i.getUser().getUsername() : null);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  LOGIN ATTEMPTS
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/login-attempts")
    @Operation(summary = "Próby logowania", description = "Filtry po userze, IP, success")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<LoginAttemptDto>> listLoginAttempts(
            @RequestParam Optional<UUID> userId,
            @RequestParam Optional<String> ip,
            @RequestParam Optional<Boolean> success,
            @RequestParam Optional<Instant> from,
            @RequestParam Optional<Instant> to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Specification<UserLoginAttempt> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            userId.ifPresent(uid -> ps.add(cb.equal(root.get("user").get("id"), uid)));
            ip.ifPresent(v -> ps.add(cb.equal(root.get("ip"), v)));
            success.ifPresent(v -> ps.add(cb.equal(root.get("success"), v)));
            from.ifPresent(f -> ps.add(cb.greaterThanOrEqualTo(root.get("timestamp"), f)));
            to.ifPresent(t -> ps.add(cb.lessThanOrEqualTo(root.get("timestamp"), t)));
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<LoginAttemptDto> result = loginAttemptRepository.findAll(spec, pageable).map(this::toDto);
        return ResponseEntity.ok(result);
    }

    private LoginAttemptDto toDto(UserLoginAttempt a) {
        return new LoginAttemptDto(
                a.getId(), a.getTimestamp(), a.getSuccess(), a.getIp(), a.getUserAgent(),
                a.getUser() != null ? a.getUser().getId() : null,
                a.getUser() != null ? a.getUser().getUsername() : null);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  SESSIONS (wszystkich użytkowników)
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/sessions")
    @Operation(summary = "Wszystkie aktywne sesje")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<AdminSessionDto>> listSessions(
            @RequestParam Optional<UUID> userId,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Instant now = Instant.now();
        Specification<UserSession> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            userId.ifPresent(uid -> ps.add(cb.equal(root.get("user").get("id"), uid)));
            if (activeOnly) {
                ps.add(cb.isNull(root.get("revokedAt")));
                ps.add(cb.greaterThan(root.get("expiresAt"), now));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "issuedAt"));
        Page<AdminSessionDto> result = sessionRepository.findAll(spec, pageable)
                .map(s -> toDto(s, now));
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Wymuś unieważnienie sesji (admin)")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID id) {
        UserSession s = sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesja nie istnieje"));
        if (s.getRevokedAt() == null) {
            s.setRevokedAt(Instant.now());
            sessionRepository.save(s);
        }
        return ResponseEntity.noContent().build();
    }

    private AdminSessionDto toDto(UserSession s, Instant now) {
        User u = s.getUser();
        return new AdminSessionDto(
                s.getId(),
                u != null ? u.getId() : null,
                u != null ? u.getUsername() : null,
                u != null ? u.getRole() : null,
                s.getIssuedAt(),
                s.getExpiresAt(),
                s.getRevokedAt(),
                s.getIp(),
                s.getUserAgent(),
                s.getRevokedAt() == null && s.getExpiresAt() != null && s.getExpiresAt().isAfter(now));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  USERS – zarządzanie kontami
    // ─────────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Lista użytkowników")
    public ResponseEntity<Page<AdminUserDto>> listUsers(
            @RequestParam Optional<String> q,
            @RequestParam Optional<String> role,
            @RequestParam Optional<Boolean> locked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Specification<User> spec = (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            q.ifPresent(s -> {
                String like = "%" + s.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            });
            role.ifPresent(r -> ps.add(cb.equal(root.get("role"), r)));
            locked.ifPresent(v -> {
                if (v) ps.add(cb.isTrue(root.get("accountLocked")));
                else ps.add(cb.or(cb.isNull(root.get("accountLocked")), cb.isFalse(root.get("accountLocked"))));
            });
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.ASC, "username"));
        Page<AdminUserDto> result = userRepository.findAll(spec, pageable).map(this::toUserDto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/users/{id}/unlock")
    @Operation(summary = "Odblokuj konto użytkownika")
    public ResponseEntity<AdminUserDto> unlockUser(@PathVariable UUID id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Użytkownik nie istnieje"));
        u.setAccountLocked(false);
        u.setFailedLoginAttempts(0);
        userRepository.save(u);
        return ResponseEntity.ok(toUserDto(u));
    }

    @PostMapping("/users/{id}/lock")
    @Operation(summary = "Zablokuj konto użytkownika")
    public ResponseEntity<AdminUserDto> lockUser(@PathVariable UUID id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Użytkownik nie istnieje"));
        u.setAccountLocked(true);
        userRepository.save(u);
        return ResponseEntity.ok(toUserDto(u));
    }

    private AdminUserDto toUserDto(User u) {
        return new AdminUserDto(
                u.getId(), u.getUsername(), u.getEmail(), u.getRole(),
                u.getAccountLocked(), u.getMfaEnabled(),
                u.getFailedLoginAttempts(),
                u.getLastLoginAt(), u.getLastLoginIp(), u.getCreatedAt());
    }
}

