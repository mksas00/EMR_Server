package com.example.emr_server.entity;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionEntityListener;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;


@Getter
@Entity
@EntityListeners(EncryptionEntityListener.class)
@Table(name = "patients", schema = "emr")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "patient.first_name")
    @Column(name = "first_name", nullable = false, length = 300)
    private String firstName;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "patient.last_name")
    @Column(name = "last_name", nullable = false, length = 300)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Encrypted(mode = Encrypted.Mode.DETERMINISTIC, value = "patient.pesel")
    @Column(name = "pesel", nullable = false, length = 200)
    private String pesel;

    @Column(name = "contact_info")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> contactInfo;

    @Encrypted(mode = Encrypted.Mode.RANDOM, value = "patient.address")
    @Column(name = "address", length = Integer.MAX_VALUE)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    public void setId(UUID id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setPesel(String pesel) {
        this.pesel = pesel;
    }

    public void setContactInfo(Map<String, Object> contactInfo) {
        this.contactInfo = contactInfo;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}