package com.example.emr_server.controller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LabOrderItemDto(
        UUID id,
        UUID orderId,
        String testCode,
        String testName,
        String unit,
        BigDecimal refRangeLow,
        BigDecimal refRangeHigh,
        String refRangeText,
        String result,
        BigDecimal resultNumeric,
        String abnormalFlag,
        String status,
        Instant completedAt,
        UUID completedById,
        String completedByUsername,
        String notes,
        Integer sortOrder
) {}

