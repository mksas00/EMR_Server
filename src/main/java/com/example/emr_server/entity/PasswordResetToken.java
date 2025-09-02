package com.example.emr_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens", schema = "emr", indexes = {
        @Index(name = "prt_token_hash_idx", columnList = "token_hash")
})
@Getter
@Setter
public class PasswordResetToken {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash; // sha256 Base64 (44) lub hex (64) – tu przyjmujemy Base64 skrócone jeśli chcesz można zmienić

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "requested_ip")
    private String requestedIp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

