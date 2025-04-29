package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "allergies", schema = "emr")
public class Allergy {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "allergen", nullable = false, length = Integer.MAX_VALUE)
    private String allergen;

    @Column(name = "reaction", length = Integer.MAX_VALUE)
    private String reaction;

    @Column(name = "severity", length = 20)
    private String severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noted_by")
    private User notedBy;

}