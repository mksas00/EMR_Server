package com.example.emr_server.repository;

import com.example.emr_server.entity.LabPanel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabPanelRepository extends JpaRepository<LabPanel, UUID> {
    Optional<LabPanel> findByCode(String code);
    List<LabPanel> findAllByIsActiveTrueOrderByNameAsc();
}

