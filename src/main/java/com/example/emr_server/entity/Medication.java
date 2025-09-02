package com.example.emr_server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "medications", schema = "emr")
public class Medication {
    @Id
    @GeneratedValue
    @UuidGenerator
    @ColumnDefault("gen_random_uuid()")
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "urpl_id")
    private Long urplId;

    @Column(name = "name", nullable = false)
    private String name; // Nazwa Produktu Leczniczego

    @Column(name = "common_name")
    private String commonName; // Nazwa powszechnie stosowana

    @Column(name = "banned_for_animals")
    private Boolean bannedForAnimals; // Zakaz stosowania u zwierząt

    @Column(name = "strength")
    private String strength; // Moc

    @Column(name = "pharmaceutical_form")
    private String pharmaceuticalForm; // Postać farmaceutyczna

    @Column(name = "authorization_number")
    private String authorizationNumber; // Nr pozwolenia

    @Column(name = "authorization_valid_to")
    private LocalDate authorizationValidTo; // Ważność pozwolenia (do)

    @Column(name = "marketing_authorization_holder")
    private String marketingAuthorizationHolder; // Podmiot odpowiedzialny

    @Column(name = "procedure_type")
    private String procedureType; // Typ procedury

    @Column(name = "legal_basis")
    private String legalBasis; // Podstawa prawna wniosku

    @Column(name = "atc_code")
    private String atcCode; // Kod ATC

    @Column(name = "active_substances")
    private String activeSubstances; // Substancja czynna (tekst)

    @Column(name = "target_species")
    private String targetSpecies; // Gatunki docelowe

    @Column(name = "packaging_consent")
    private String packagingConsent; // Opakowanie - Zgody Prezesa

    @Column(name = "prescription_category")
    private String prescriptionCategory; // Kat. dost.

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at")
    private Instant createdAt;
}
