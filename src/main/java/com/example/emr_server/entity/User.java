package com.example.emr_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@EntityListeners(EncryptionEntityListener.class)
@Table(name = "users", schema = "emr")
public class User {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    // Security extensions (V1/V2)
    @Column(name = "last_password_change")
    private Instant lastPasswordChange;

    @Column(name = "is_account_locked")
    private Boolean accountLocked;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "user.mfa_secret")
    @Column(name = "mfa_secret", length = 256)
    private String mfaSecret;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @Column(name = "password_algo", length = 30)
    private String passwordAlgo;

    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled;
}

