package com.example.emr_server.repository;

import com.example.emr_server.entity.PasswordResetToken;
import com.example.emr_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
    void deleteByExpiresAtBefore(Instant cutoff);
    long countByUserAndCreatedAtAfter(User user, Instant after);
}

