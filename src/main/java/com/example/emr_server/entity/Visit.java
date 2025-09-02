package com.example.emr_server.entity;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(EncryptionEntityListener.class)
@Getter
@Setter
@Table(name = "visits", schema = "emr")
public class Visit {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "visit_date", nullable = false)
    private Instant visitDate;

    // Nowe: czas zakończenia wizyty
    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "visit_type", length = 50)
    private String visitType;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "visit.reason")
    @Column(name = "reason", length = Integer.MAX_VALUE)
    private String reason;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "visit.diagnosis")
    @Column(name = "diagnosis", length = Integer.MAX_VALUE)
    private String diagnosis;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "visit.notes")
    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

    @ColumnDefault("false")
    @Column(name = "is_confidential")
    private Boolean isConfidential;

    // Nowe: status wizyty
    public enum Status { PLANNED, CONFIRMED, COMPLETED, CANCELED }

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private Status status;

    public Boolean getConfidential() {
        return isConfidential;
    }
    public void setConfidential(Boolean confidential) {
        isConfidential = confidential;
    }
}