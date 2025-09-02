package com.example.emr_server.entity;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@EntityListeners(EncryptionEntityListener.class)
@Table(name = "medication_history", schema = "emr")
public class MedicationHistory {
    @Id
    @GeneratedValue
    @UuidGenerator
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Powiązanie z katalogiem leków (opcjonalne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id")
    private Medication medicationRef;


    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "medhist.reason")
    @Column(name = "reason", length = Integer.MAX_VALUE)
    private String reason;

}