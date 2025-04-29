package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_log", schema = "emr")
public class AuditLog {
    @Id
    @ColumnDefault("nextval('emr.audit_log_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "\"timestamp\"")
    private Instant timestamp;

}