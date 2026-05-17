package com.example.emr_server.security;

import com.example.emr_server.entity.User;
import com.example.emr_server.entity.UserLoginAttempt;
import com.example.emr_server.repository.UserLoginAttemptRepository;
import com.example.emr_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Zapis próby logowania w niezależnej transakcji (REQUIRES_NEW)
 * + heurystyki detekcji ataków na warstwę uwierzytelniania.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptRecorder {

    private static final int BRUTE_FORCE_THRESHOLD = 5;        // failów z 1 IP w 5 min
    private static final int BRUTE_FORCE_WINDOW_MIN = 5;
    private static final int CREDENTIAL_STUFFING_DISTINCT = 3;  // failów na 3+ kont z 1 IP
    private static final int CREDENTIAL_STUFFING_WINDOW_MIN = 10;

    private final UserLoginAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final SecurityIncidentService incidents;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User user, boolean success, String ip, String userAgent) {
        UserLoginAttempt attempt = new UserLoginAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setUser(user);
        attempt.setSuccess(success);
        attempt.setIp(ip);
        attempt.setUserAgent(userAgent != null ? userAgent : "api");
        attempt.setTimestamp(Instant.now());
        attemptRepository.save(attempt);
        if (!success && user != null) {
            int fails = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(fails);
            userRepository.save(user);
        }

        if (!success && ip != null) {
            try {
                detectAttacks(ip, user);
            } catch (Exception ex) {
                log.debug("attack detection skipped: {}", ex.getMessage());
            }
        }
    }

    /**
     * Heurystyki uruchamiane po każdej nieudanej próbie:
     *  - BRUTE_FORCE_BURST: gęsta seria failów z 1 IP
     *  - CREDENTIAL_STUFFING: failowanie na wielu różnych kontach z 1 IP
     */
    private void detectAttacks(String ip, User user) {
        Instant now = Instant.now();
        Instant bfSince = now.minus(BRUTE_FORCE_WINDOW_MIN, ChronoUnit.MINUTES);
        long bfCount = attemptRepository.countByIpAndSuccessFalseAndTimestampAfter(ip, bfSince);
        if (bfCount == BRUTE_FORCE_THRESHOLD) {
            // Tylko gdy DOKŁADNIE osiągnęliśmy próg – nie spamujemy incydentami
            incidents.critical(
                    SecurityIncidentService.Cat.BRUTE_FORCE_BURST,
                    String.format("Wykryto %d nieudanych logowań z IP %s w ciągu %d min",
                            bfCount, ip, BRUTE_FORCE_WINDOW_MIN),
                    user);
        }

        Instant csSince = now.minus(CREDENTIAL_STUFFING_WINDOW_MIN, ChronoUnit.MINUTES);
        long csDistinct = attemptRepository.countDistinctUsersFailingFromIp(ip, csSince);
        if (csDistinct == CREDENTIAL_STUFFING_DISTINCT) {
            incidents.critical(
                    SecurityIncidentService.Cat.CREDENTIAL_STUFFING,
                    String.format("IP %s próbowało zalogować się na %d różnych kont w ciągu %d min",
                            ip, csDistinct, CREDENTIAL_STUFFING_WINDOW_MIN),
                    user);
        }
    }
}


