package com.example.emr_server.repository;

import com.example.emr_server.entity.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository extends JpaRepository<Medication, UUID>, JpaSpecificationExecutor<Medication> {

    Optional<Medication> findByAuthorizationNumber(String authorizationNumber);
    Optional<Medication> findByUrplId(Long urplId);

    @Query("select m from Medication m where (:q is null or lower(m.name) like lower(concat('%', :q, '%')) or lower(m.commonName) like lower(concat('%', :q, '%'))) and (:atc is null or m.atcCode = :atc)")
    Page<Medication> search(@Param("q") String q, @Param("atc") String atc, Pageable pageable);
}
