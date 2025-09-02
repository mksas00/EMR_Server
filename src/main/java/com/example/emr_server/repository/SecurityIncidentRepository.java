package com.example.emr_server.repository;

import com.example.emr_server.entity.SecurityIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, UUID> {
    List<SecurityIncident> findByStatusOrderByDetectedAtDesc(String status);
    List<SecurityIncident> findTop50ByOrderByDetectedAtDesc();
}

