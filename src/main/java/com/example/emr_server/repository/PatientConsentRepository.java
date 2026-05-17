package com.example.emr_server.repository;

import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientConsentRepository
        extends JpaRepository<PatientConsent, UUID>, JpaSpecificationExecutor<PatientConsent> {

    Optional<PatientConsent> findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(
            Patient patient, User grantedTo, String scope);

    List<PatientConsent> findByGrantedToAndRevokedAtIsNull(User user);
    List<PatientConsent> findByPatientAndRevokedAtIsNull(Patient patient);

    Page<PatientConsent> findByPatient_IdOrderByGrantedAtDesc(UUID patientId, Pageable pageable);

    /** Aktywne zgody dla grantedTo (nieanulowane i nieprzedawnione lub bez wygasania). */
    @Query("""
        SELECT c FROM PatientConsent c
        WHERE c.grantedTo.id = :userId
          AND c.revokedAt IS NULL
          AND (c.expiresAt IS NULL OR c.expiresAt > :now)
        ORDER BY c.grantedAt DESC
        """)
    Page<PatientConsent> findActiveConsentsByGrantedTo(UUID userId, Instant now, Pageable pageable);

    Page<PatientConsent> findByGrantedTo_IdOrderByGrantedAtDesc(UUID userId, Pageable pageable);

    long countByGrantedToAndScopeAndGrantedAtAfter(User grantedTo, String scope, Instant after);

    boolean existsByPatient_IdAndGrantedTo_IdAndScopeAndRevokedAtIsNull(
            UUID patientId, UUID grantedToId, String scope);
}

