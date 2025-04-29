package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "data_versions", schema = "emr")
public class DataVersion {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    @Column(name = "old_data")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> oldData;

    @Column(name = "new_data")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> newData;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "modified_at")
    private Instant modifiedAt;

}