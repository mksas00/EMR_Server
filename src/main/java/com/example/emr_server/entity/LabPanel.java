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
 * Predefiniowany panel laboratoryjny (np. "Morfologia", "Lipidogram").
 * Pozwala lekarzowi jednym kliknięciem dodać zestaw testów do zlecenia.
 */
@Getter
@Setter
@Entity
@Table(name = "lab_panels", schema = "emr")
public class LabPanel {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "panel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<LabPanelItem> items = new ArrayList<>();
}

