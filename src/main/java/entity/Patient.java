package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "patients", schema = "emr")
public class Patient {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "pesel", nullable = false, length = 11)
    private String pesel;

    @Column(name = "contact_info")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> contactInfo;

    @Column(name = "address", length = Integer.MAX_VALUE)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Users createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

}