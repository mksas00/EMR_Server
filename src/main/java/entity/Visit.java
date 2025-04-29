package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "visits", schema = "emr")
public class Visit {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "visit_date", nullable = false)
    private Instant visitDate;

    @Column(name = "visit_type", length = 50)
    private String visitType;

    @Column(name = "reason", length = Integer.MAX_VALUE)
    private String reason;

    @Column(name = "diagnosis", length = Integer.MAX_VALUE)
    private String diagnosis;

    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

    @ColumnDefault("false")
    @Column(name = "is_confidential")
    private Boolean isConfidential;

}