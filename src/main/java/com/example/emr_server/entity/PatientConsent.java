package com.example.emr_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "patient_consents", schema = "emr")
public class PatientConsent {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_to_user_id")
    private User grantedTo;

    @Column(name = "scope", nullable = false, length = 100)
    private String scope;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
