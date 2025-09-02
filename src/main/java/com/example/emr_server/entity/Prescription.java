package com.example.emr_server.entity;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@EntityListeners(EncryptionEntityListener.class)
@Table(name = "prescriptions", schema = "emr")
public class Prescription {
    @Id
    @GeneratedValue
    @UuidGenerator
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    // Pozycje leków (nowy model)
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionMedication> items = new ArrayList<>();

    public void setItems(List<PrescriptionMedication> items) {
        this.items.clear();
        if (items != null) items.forEach(this::addItem);
    }

    public void addItem(PrescriptionMedication item) {
        if (item == null) return;
        item.setPrescription(this);
        this.items.add(item);
    }

    public void removeItem(PrescriptionMedication item) {
        if (item == null) return;
        item.setPrescription(null);
        this.items.remove(item);
    }

    // Opcjonalne ogólne zalecenia dla całej recepty
    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "prescription.dosage_info")
    @Column(name = "dosage_info", length = Integer.MAX_VALUE)
    private String dosageInfo;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @ColumnDefault("false")
    @Column(name = "is_repeatable")
    private Boolean isRepeatable;

}