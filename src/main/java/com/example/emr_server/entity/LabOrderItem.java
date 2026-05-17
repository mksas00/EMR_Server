package com.example.emr_server.entity;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pojedyncza pozycja zlecenia laboratoryjnego (jeden test).
 * test_name i result szyfrowane (zgodnie z polityką EMR).
 */
@Getter
@Setter
@Entity
@EntityListeners(EncryptionEntityListener.class)
@Table(name = "lab_order_items", schema = "emr")
public class LabOrderItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private LabOrder order;

    @Column(name = "test_code", nullable = false, length = 40)
    private String testCode;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "lab.test_name")
    @Column(name = "test_name", nullable = false, length = Integer.MAX_VALUE)
    private String testName;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "ref_range_low", precision = 14, scale = 4)
    private BigDecimal refRangeLow;

    @Column(name = "ref_range_high", precision = 14, scale = 4)
    private BigDecimal refRangeHigh;

    @Column(name = "ref_range_text", columnDefinition = "text")
    private String refRangeText;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "lab.result")
    @Column(name = "result", length = Integer.MAX_VALUE)
    private String result;

    /** Wartość liczbowa wyniku (jeśli dało się sparsować) – do wyliczania abnormal flag i wykresów. */
    @Column(name = "result_numeric", precision = 14, scale = 4)
    private BigDecimal resultNumeric;

    /** 'L' (low), 'H' (high), 'N' (normal), 'LL'/'HH' (krytycznie). */
    @Column(name = "abnormal_flag", length = 2)
    private String abnormalFlag;

    @ColumnDefault("'pending'")
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @ColumnDefault("0")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = "pending";
        if (sortOrder == null) sortOrder = 0;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

