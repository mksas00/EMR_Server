package com.example.emr_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pozycja w predefiniowanym panelu laboratoryjnym (szablon).
 * Nie zawiera wyników – jest tylko wzorcem do tworzenia LabOrderItem.
 */
@Getter
@Setter
@Entity
@Table(name = "lab_panel_items", schema = "emr")
public class LabPanelItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panel_id", nullable = false)
    private LabPanel panel;

    @Column(name = "test_code", length = 40, nullable = false)
    private String testCode;

    @Column(name = "test_name", length = 200, nullable = false)
    private String testName;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "ref_range_low", precision = 14, scale = 4)
    private BigDecimal refRangeLow;

    @Column(name = "ref_range_high", precision = 14, scale = 4)
    private BigDecimal refRangeHigh;

    @Column(name = "ref_range_text", columnDefinition = "text")
    private String refRangeText;

    @ColumnDefault("0")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}

