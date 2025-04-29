package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
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

    @Column(name = "test_name", nullable = false, length = Integer.MAX_VALUE)
    private String testName;

    @Column(name = "result", length = Integer.MAX_VALUE)
    private String result;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "reference_range", length = Integer.MAX_VALUE)
    private String referenceRange;

    @ColumnDefault("'completed'")
    @Column(name = "status", length = 20)
    private String status;

}