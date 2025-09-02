package com.example.emr_server.security;

import com.example.emr_server.entity.*;
import com.example.emr_server.repository.*;
import com.example.emr_server.security.dto.AuthRequest;
import com.example.emr_server.security.dto.AuthResponse;
import com.example.emr_server.security.dto.RefreshRequest;
import com.example.emr_server.security.dto.LogoutRequest;
import com.example.emr_server.security.dto.SessionResponse;
import com.example.emr_server.security.dto.ChangePasswordRequest;
import com.example.emr_server.security.dto.MfaSetupResponse;
import com.example.emr_server.security.dto.MfaConfirmRequest;
import com.example.emr_server.security.dto.MfaConfirmResponse;
import com.example.emr_server.security.dto.MfaStatusResponse;
import com.example.emr_server.security.dto.MfaRecoveryCodesResponse;
import com.example.emr_server.security.dto.PasswordResetRequestDto;
import com.example.emr_server.security.dto.PasswordResetConfirmDto;
import com.example.emr_server.security.mfa.MfaService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserLoginAttemptRepository attemptRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityAuthProperties authProps;
    private final AuditLogRepository auditLogRepository;
    private final SecurityIncidentRepository securityIncidentRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final MfaService mfaService;
    private final PasswordResetTokenRepository passwordResetTokenRepository; // nowe

    @Value("${security.jwt.refresh-ttl-days}")
    private long refreshTtlDays;

    @Value("${security.jwt.access-ttl-minutes}")
    private long accessTtlMinutes;

    @Transactional
    public AuthResponse login(AuthRequest req, String ip, String userAgent) {
        // Drugi krok MFA gdy challengeToken obecny
        if (req.getChallengeToken() != null) {
            return completeMfa(req, ip, userAgent);
        }
        String principal = req.getUsernameOrEmail();
        if (principal == null || principal.isBlank() || req.getPassword() == null) {
            log.debug("LOGIN_INVALID_INPUT principal='{}' ip={}", principal, ip);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak danych logowania");
        }
        principal = principal.trim();
        User user;
        try {
            user = findUser(principal);
        } catch (ResponseStatusException ex) {
            log.info("LOGIN_USER_NOT_FOUND principal='{}' ip={}", principal, ip);
            throw ex;
        }
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            createAudit(user, "LOGIN_LOCKED", "Próba logowania na zablokowane konto");
            log.warn("LOGIN_LOCKED principal='{}' ip={}", principal, ip);
            throw new ResponseStatusException(HttpStatus.LOCKED, "Konto zablokowane");
        }
        String storedHash = user.getPasswordHash();
        if (storedHash != null) {
            String orig = storedHash;
            storedHash = storedHash.trim();
            if (!orig.equals(storedHash)) {
                log.debug("LOGIN_HASH_TRIMMED principal='{}' origLen={} newLen={}", principal, orig.length(), storedHash.length());
            }
        }
        boolean pwdOk = passwordEncoder.matches(req.getPassword(), storedHash);
        recordAttempt(user, pwdOk, ip, principal);
        if (!pwdOk) {
            createAudit(user, "LOGIN_FAILURE", "Nieudane logowanie (hasło)");
            applyLockPolicy(user);
            log.info("LOGIN_FAILURE principal='{}' ip={} fails={} locked={}", principal, ip, user.getFailedLoginAttempts(), user.getAccountLocked());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Błędne dane logowania");
        }
        // Hasło OK – jeśli MFA włączone i brak kodu -> wydaj challenge
        if (mfaService.isEnabled(user) && (req.getMfaCode() == null || req.getMfaCode().isBlank())) {
            String challenge = jwtService.generateMfaChallengeToken(user.getId());
            log.info("LOGIN_MFA_CHALLENGE principal='{}' ip={}", principal, ip);
            return AuthResponse.builder()
                    .mfaRequired(true)
                    .challengeToken(challenge)
                    .accessToken(null)
                    .refreshToken(null)
                    .expiresInSeconds(0)
                    .build();
        }
        // Hasło OK + MFA wyłączone LUB kod dostarczony
        if (mfaService.isEnabled(user)) {
            boolean totpOk = mfaService.verifyTotp(user.getMfaSecret(), req.getMfaCode());
            if (!totpOk) {
                createAudit(user, "LOGIN_MFA_FAIL", "Niepoprawny kod MFA");
                log.info("LOGIN_MFA_FAIL principal='{}' ip={}", principal, ip);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kod MFA niepoprawny");
            }
        }
        finalizeSuccessfulLogin(user, ip);
        log.info("LOGIN_SUCCESS principal='{}' ip={}", principal, ip);
        return issueTokens(user, ip, userAgent);
    }

    private AuthResponse completeMfa(AuthRequest req, String ip, String userAgent) {
        String challengeToken = req.getChallengeToken();
        Claims claims;
        try {
            claims = jwtService.parse(challengeToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Challenge niepoprawny");
        }
        if (!jwtService.isMfaChallenge(claims)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token nie jest wyzwaniem MFA");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Użytkownik nie istnieje"));
        if (!mfaService.isEnabled(user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA nieaktywne");
        }
        // Weryfikacja kodu lub recovery
        boolean ok = mfaService.verifyTotp(user.getMfaSecret(), req.getMfaCode());
        if (!ok && req.getMfaCode() != null) {
            // spróbuj recovery (kod może być alfanumeryczny 10 znaków)
            ok = mfaService.consumeRecoveryCode(user, req.getMfaCode());
        }
        if (!ok) {
            createAudit(user, "LOGIN_MFA_FAIL", "Niepoprawny kod MFA (2nd step)");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Kod MFA niepoprawny");
        }
        finalizeSuccessfulLogin(user, ip);
        createAudit(user, "LOGIN_SUCCESS", "Udane logowanie (MFA)");
        log.info("LOGIN_SUCCESS_MFA user={} ip={}", user.getUsername(), ip);
        return issueTokens(user, ip, userAgent);
    }

    private void finalizeSuccessfulLogin(User user, String ip) {
        resetFailures(user, ip);
        createAudit(user, "LOGIN_PWD_OK", "Hasło poprawne");
        if (mfaService.isEnabled(user)) {
            createAudit(user, "LOGIN_MFA_OK", "MFA poprawne");
        }
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req, String ip, String userAgent) {
        if (req.getRefreshToken() == null || req.getRefreshToken().isBlank()) {
            log.debug("REFRESH_NO_TOKEN ip={}", ip);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak refreshToken");
        }
        String provided = req.getRefreshToken().trim();
        String hash = HashUtil.sha256Base64(provided);
        UserSession session = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> {
                    log.info("REFRESH_UNKNOWN ip={}", ip);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh nieznany");
                });
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(Instant.now())) {
            log.info("REFRESH_INVALID ip={} revoked={} expiresAt={}", ip, session.getRevokedAt(), session.getExpiresAt());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh nieważny");
        }
        User user = session.getUser();
        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            createIncident(user, "high", "REFRESH_ON_LOCKED", "Próba refresh na zablokowanym koncie");
            createAudit(user, "REFRESH_LOCKED", "Próba odświeżenia tokenu gdy konto zablokowane");
            log.warn("REFRESH_LOCKED user={} ip={}", user.getUsername(), ip);
            throw new ResponseStatusException(HttpStatus.LOCKED, "Konto zablokowane");
        }
        session.setRevokedAt(Instant.now());
        sessionRepository.save(session);
        createAudit(user, "REFRESH", "Odświeżenie tokenu");
        log.info("REFRESH_SUCCESS user={} ip={}", user.getUsername(), ip);
        return issueTokens(user, ip, userAgent);
    }

    @Transactional
    public void logout(LogoutRequest req) {
        if (req.getRefreshToken() == null) return;
        String hash = HashUtil.sha256Base64(req.getRefreshToken());
        sessionRepository.findByRefreshTokenHash(hash).ifPresent(s -> {
            s.setRevokedAt(Instant.now());
            sessionRepository.save(s);
            createAudit(s.getUser(), "LOGOUT", "Wylogowanie / unieważnienie refresh");
        });
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listOwnSessions() {
        User current = currentUser();
        Instant now = Instant.now();
        return sessionRepository.findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByExpiresAtDesc(current, now)
                .stream()
                .map(s -> SessionResponse.builder()
                        .id(s.getId())
                        .issuedAt(s.getIssuedAt())
                        .expiresAt(s.getExpiresAt())
                        .revokedAt(s.getRevokedAt())
                        .ip(s.getIp())
                        .userAgent(s.getUserAgent())
                        .active(s.getRevokedAt() == null && s.getExpiresAt().isAfter(now))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        User current = currentUser();
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesja nie istnieje"));
        if (!session.getUser().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Brak uprawnień do sesji");
        }
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(Instant.now());
            sessionRepository.save(session);
            createAudit(current, "SESSION_REVOKE", "Revokacja sesji " + sessionId);
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        if (req == null || req.getCurrentPassword() == null || req.getNewPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak danych hasła");
        }
        User user = currentUser();
        // Weryfikacja aktualnego
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            createAudit(user, "PASSWORD_CHANGE_FAIL", "Błędne aktualne hasło");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Aktualne hasło niepoprawne");
        }
        // Polityka
        var errors = passwordPolicyService.validate(user, req.getNewPassword());
        if (!errors.isEmpty()) {
            createAudit(user, "PASSWORD_POLICY_REJECT", String.join(", ", errors));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", errors));
        }
        // Zapis starego hasła do historii (jeśli istnieje)
        if (user.getPasswordHash() != null) {
            PasswordHistory ph = new PasswordHistory();
            ph.setId(UUID.randomUUID());
            ph.setUser(user);
            ph.setPasswordHash(user.getPasswordHash());
            ph.setPasswordAlgo(user.getPasswordAlgo() == null ? "bcrypt" : user.getPasswordAlgo());
            ph.setCreatedAt(Instant.now());
            passwordHistoryRepository.save(ph);
        }
        // Aktualizacja hasła
        String newHash = passwordEncoder.encode(req.getNewPassword());
        user.setPasswordHash(newHash);
        user.setPasswordAlgo("bcrypt");
        user.setLastPasswordChange(Instant.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        createAudit(user, "PASSWORD_CHANGE", "Zmiana hasła");
    }

    // ============ PASSWORD RESET ============
    @Transactional
    public void requestPasswordReset(PasswordResetRequestDto dto, String ip) {
        if (dto == null || dto.getUsernameOrEmail() == null || dto.getUsernameOrEmail().isBlank()) {
            return; // ciche wyjście (nie ujawniamy)
        }
        String principal = dto.getUsernameOrEmail().trim();
        User user = null;
        try { user = findUser(principal); } catch (Exception ignored) { }
        if (user == null) {
            // log ogólny bez wskazania nieistniejącego użytk.
            log.info("PASSWORD_RESET_REQUEST principal='{}' ip={}", principal, ip);
            return;
        }
        // Limit lokalny: max 3 aktywne/świeże w 15 min (opcjonalnie)
        long recent = passwordResetTokenRepository.countByUserAndCreatedAtAfter(user, Instant.now().minus(15, ChronoUnit.MINUTES));
        if (recent >= 3) {
            createAudit(user, "PASSWORD_RESET_REQUEST_BLOCKED", "Przekroczono limit żądań resetu");
            return;
        }
        byte[] raw = new byte[32];
        new java.security.SecureRandom().nextBytes(raw);
        String tokenRaw = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String tokenHash = HashUtil.sha256Base64(tokenRaw);
        com.example.emr_server.entity.PasswordResetToken prt = new com.example.emr_server.entity.PasswordResetToken();
        prt.setId(java.util.UUID.randomUUID());
        prt.setUser(user);
        prt.setTokenHash(tokenHash);
        prt.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        prt.setCreatedAt(Instant.now());
        prt.setRequestedIp(ip);
        passwordResetTokenRepository.save(prt);
        createAudit(user, "PASSWORD_RESET_REQUEST", "Wygenerowano token resetu (nie wysyłamy maila – log)");
        log.info("PASSWORD_RESET_TOKEN user={} token={}", user.getUsername(), tokenRaw); // do celów pracy – w realu wysyłka maila
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDto dto, String ip) {
        if (dto == null || dto.getToken() == null || dto.getNewPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brak danych");
        }
        String tokenHash = HashUtil.sha256Base64(dto.getToken().trim());
        var opt = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(tokenHash);
        if (opt.isEmpty()) {
            // celowo nie wskazujemy powodu
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token nieważny");
        }
        var prt = opt.get();
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token wygasł");
        }
        User user = prt.getUser();
        // Polityka hasła
        var errors = passwordPolicyService.validate(user, dto.getNewPassword());
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", errors));
        }
        // Historia
        if (user.getPasswordHash() != null) {
            PasswordHistory ph = new PasswordHistory();
            ph.setId(UUID.randomUUID());
            ph.setUser(user);
            ph.setPasswordHash(user.getPasswordHash());
            ph.setPasswordAlgo(user.getPasswordAlgo() == null ? "bcrypt" : user.getPasswordAlgo());
            ph.setCreatedAt(Instant.now());
            passwordHistoryRepository.save(ph);
        }
        // Ustaw nowe hasło
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordAlgo("bcrypt");
        user.setLastPasswordChange(Instant.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        // Oznacz token zużyty
        prt.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(prt);
        // Revoke wszystkie sesje
        var sessions = sessionRepository.findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByExpiresAtDesc(user, Instant.now());
        sessions.forEach(s -> { s.setRevokedAt(Instant.now()); sessionRepository.save(s); });
        createAudit(user, "PASSWORD_RESET_SUCCESS", "Reset hasła zakończony sukcesem (IP=" + ip + ")");
        log.info("PASSWORD_RESET_SUCCESS user={} ip={}", user.getUsername(), ip);
    }
    // ============ END PASSWORD RESET ==========

    private User findUser(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return userRepository.findByEmail(usernameOrEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Użytkownik nie istnieje"));
        }
        return userRepository.findByUsername(usernameOrEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Użytkownik nie istnieje"));
    }

    private void recordAttempt(User user, boolean success, String ip, String userNameOrEmail) {
        UserLoginAttempt attempt = new UserLoginAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setUser(user);
        attempt.setSuccess(success);
        attempt.setIp(ip);
        attempt.setUserAgent("api");
        attempt.setTimestamp(Instant.now());
        attemptRepository.save(attempt);
        if (!success) {
            int fails = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(fails);
            userRepository.save(user);
        }
    }

    private void applyLockPolicy(User user) {
        Instant windowStart = Instant.now().minus(authProps.getWindowMinutes(), ChronoUnit.MINUTES);
        long recentFails = attemptRepository.countByUserAndSuccessIsFalseAndTimestampAfter(user, windowStart);
        if (recentFails >= authProps.getMaxFailed()) {
            user.setAccountLocked(true);
            userRepository.save(user);
            createIncident(user, "high", "ACCOUNT_LOCK", "Zablokowano po " + recentFails + " nieudanych próbach w oknie");
            createAudit(user, "ACCOUNT_LOCK", "Konto zablokowane po nieudanych logowaniach");
        }
    }

    private void createIncident(User user, String severity, String category, String description) {
        SecurityIncident si = new SecurityIncident();
        si.setId(UUID.randomUUID());
        si.setDetectedAt(Instant.now());
        si.setSeverity(severity);
        si.setCategory(category);
        si.setDescription(description);
        si.setUser(user);
        si.setStatus("open");
        securityIncidentRepository.save(si);
    }

    private void createAudit(User user, String action, String description) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setDescription(description);
        log.setUser(user);
        log.setTimestamp(Instant.now());
        auditLogRepository.save(log);
    }

    private void resetFailures(User user, String ip) {
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);
    }

    private AuthResponse issueTokens(User user, String ip, String userAgent) {
        String access = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshRaw = UUID.randomUUID().toString();
        String refreshHash = HashUtil.sha256Base64(refreshRaw);
        Instant now = Instant.now();
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setIssuedAt(now);
        session.setExpiresAt(now.plus(refreshTtlDays, ChronoUnit.DAYS));
        session.setRefreshTokenHash(refreshHash);
        if (ip != null && !ip.isBlank()) session.setIp(ip);
        if (userAgent != null && !userAgent.isBlank()) {
            String ua = userAgent.length() > 300 ? userAgent.substring(0,300) : userAgent;
            session.setUserAgent(ua);
        }
        sessionRepository.save(session);
        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refreshRaw)
                .expiresInSeconds(accessTtlMinutes * 60)
                .build();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Brak autentykacji");
        }
        return cud.getDomainUser();
    }

    // ================= MFA API =================
    @Transactional
    public MfaSetupResponse startMfaSetup() {
        User user = currentUser();
        try {
            var start = mfaService.startSetup(user);
            createAudit(user, "MFA_SETUP_START", "Rozpoczęcie konfiguracji MFA");
            return MfaSetupResponse.builder()
                    .secret(start.secret())
                    .otpauthUri(start.otpauthUri())
                    .build();
        } catch (IllegalStateException ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
    public MfaConfirmResponse confirmMfaSetup(MfaConfirmRequest req) {
        if (req == null || req.getCode() == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Brak kodu");
        }
        User user = currentUser();
        try {
            var result = mfaService.confirmSetup(user, req.getCode());
            createAudit(user, "MFA_SETUP_CONFIRM", "Potwierdzenie konfiguracji MFA");
            return MfaConfirmResponse.builder()
                    .enabled(result.enabled())
                    .recoveryCodes(result.recoveryCodes())
                    .build();
        } catch (IllegalStateException ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
    public void disableMfa() {
        User user = currentUser();
        if (!mfaService.isEnabled(user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "MFA nieaktywne");
        }
        mfaService.disable(user);
        createAudit(user, "MFA_DISABLE", "Wyłączenie MFA");
    }
    // ================= END MFA API =============

    @Transactional(readOnly = true)
    public MfaStatusResponse mfaStatus() {
        User user = currentUser();
        boolean enabled = mfaService.isEnabled(user);
        int count = enabled ? mfaService.countActiveRecoveryCodes(user) : 0;
        return MfaStatusResponse.builder().enabled(enabled).activeRecoveryCodes(count).build();
    }

    @Transactional
    public MfaRecoveryCodesResponse regenerateRecoveryCodes() {
        User user = currentUser();
        if (!mfaService.isEnabled(user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "MFA nieaktywne");
        }
        var codes = mfaService.regenerateRecoveryCodes(user);
        createAudit(user, "MFA_RECOVERY_REGEN", "Regeneracja kodów odzyskiwania");
        return MfaRecoveryCodesResponse.builder().recoveryCodes(codes).build();
    }
}
