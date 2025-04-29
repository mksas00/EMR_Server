package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class Users {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, length = 50)
    private String password;

    @Column(name = "password_hash", nullable = false, length = Integer.MAX_VALUE)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

}
