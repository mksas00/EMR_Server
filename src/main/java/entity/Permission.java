package entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "permissions", schema = "emr")
public class Permission {
    @Id
    @ColumnDefault("nextval('emr.permissions_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "resource", nullable = false, length = 50)
    private String resource;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

}