package com.example.emr_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_login_attempts", schema = "emr")
public class UserLoginAttempt {
    @Id
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ts")
    private Instant timestamp;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @JdbcTypeCode(SqlTypes.INET)
    @Column(name = "ip")
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;
}
