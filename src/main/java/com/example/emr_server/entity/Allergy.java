package com.example.emr_server.entity;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@EntityListeners(EncryptionEntityListener.class)
@Table(name = "allergies", schema = "emr")
public class Allergy {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "allergy.allergen")
    @Column(name = "allergen", nullable = false, length = Integer.MAX_VALUE)
    private String allergen;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "allergy.reaction")
    @Column(name = "reaction", length = Integer.MAX_VALUE)
    private String reaction;

    @Column(name = "severity", length = 20)
    private String severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noted_by")
    private User notedBy;

}