package com.example.emr_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lab order – nagłówek zlecenia badań laboratoryjnych.
 * Klinicznie: jeden lekarz zleca dla pacjenta zestaw testów (LabOrderItem).
 * Status agregowany: pending → in_progress → completed (lub cancelled).
 */
@Getter
@Setter
@Entity
@Table(name = "lab_orders", schema = "emr")
public class LabOrder {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordered_by", nullable = false)
    private User orderedBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @ColumnDefault("'routine'")
    @Column(name = "priority", length = 10, nullable = false)
    private String priority;

    @ColumnDefault("'pending'")
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_reason", columnDefinition = "text")
    private String cancelledReason;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<LabOrderItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (orderedAt == null) orderedAt = now;
        if (priority == null) priority = "routine";
        if (status == null) status = "pending";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

