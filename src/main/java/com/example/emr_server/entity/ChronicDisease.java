package com.example.emr_server.entity;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@EntityListeners(EncryptionEntityListener.class)
@Table(name = "chronic_diseases", schema = "emr")
public class ChronicDisease {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "chronic.disease_name")
    @Column(name = "disease_name", nullable = false, length = Integer.MAX_VALUE)
    private String diseaseName;

    @Column(name = "diagnosed_date")
    private LocalDate diagnosedDate;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "chronic.notes")
    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

}