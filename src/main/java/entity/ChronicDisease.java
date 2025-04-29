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
@Table(name = "chronic_diseases", schema = "emr")
public class ChronicDisease {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "disease_name", nullable = false, length = Integer.MAX_VALUE)
    private String diseaseName;

    @Column(name = "diagnosed_date")
    private LocalDate diagnosedDate;

    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

}