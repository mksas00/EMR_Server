package com.example.emr_server.repository;

import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID> {
    Optional<PatientConsent> findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(Patient patient, User grantedTo, String scope);
    List<PatientConsent> findByGrantedToAndRevokedAtIsNull(User user);
    List<PatientConsent> findByPatientAndRevokedAtIsNull(Patient patient);
}

