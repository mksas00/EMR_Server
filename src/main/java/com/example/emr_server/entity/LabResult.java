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
@Table(name = "lab_results", schema = "emr")
public class LabResult {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_by")
    private User orderedBy;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "lab.test_name")
    @Column(name = "test_name", nullable = false, length = Integer.MAX_VALUE)
    private String testName;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "lab.result")
    @Column(name = "result", length = Integer.MAX_VALUE)
    private String result;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "unit", length = 20)
    private String unit;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "lab.reference_range")
    @Column(name = "reference_range", length = Integer.MAX_VALUE)
    private String referenceRange;

    @ColumnDefault("'completed'")
    @Column(name = "status", length = 20)
    private String status;

}