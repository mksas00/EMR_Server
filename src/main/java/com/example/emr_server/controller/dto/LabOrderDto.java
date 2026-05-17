package com.example.emr_server.controller.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LabOrderDto(
        UUID id,
        UUID patientId,
        String patientFullName,
        UUID orderedById,
        String orderedByUsername,
        Instant orderedAt,
        String priority,
        String status,
        String notes,
        Instant cancelledAt,
        UUID cancelledById,
        String cancelledByUsername,
        String cancelledReason,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        int itemCount,
        int completedItemCount,
        int abnormalItemCount,
        List<LabOrderItemDto> items // null gdy lista (oszczędność), wypełniane w detail
) {}

