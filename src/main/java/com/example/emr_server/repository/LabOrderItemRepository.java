package com.example.emr_server.repository;

import com.example.emr_server.entity.LabOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, UUID> {
    List<LabOrderItem> findByOrder_IdOrderBySortOrderAsc(UUID orderId);
}

