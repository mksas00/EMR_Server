package com.example.emr_server.repository;

import com.example.emr_server.entity.MedicationPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedicationPackageRepository extends JpaRepository<MedicationPackage, UUID> {
    Optional<MedicationPackage> findByGtin(String gtin);
}

