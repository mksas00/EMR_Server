package com.example.emr_server.repository;

import com.example.emr_server.entity.User;
import com.example.emr_server.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByExpiresAtDesc(User user, Instant now);
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    void deleteByUser(User user);
}

