package com.example.emr_server.repository;

import com.example.emr_server.entity.LabOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface LabOrderRepository extends JpaRepository<LabOrder, UUID>, JpaSpecificationExecutor<LabOrder> {
    long countByPatient_Id(UUID patientId);
}

