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
@Table(name = "prescriptions", schema = "emr")
public class Prescription {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "medication", nullable = false, length = Integer.MAX_VALUE)
    private String medication;

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