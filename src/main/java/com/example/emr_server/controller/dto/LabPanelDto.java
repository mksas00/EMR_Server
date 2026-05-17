package com.example.emr_server.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LabPanelDto(
        UUID id,
        String code,
        String name,
        String description,
        Boolean isActive,
        List<Item> items
) {
    public record Item(
            UUID id,
            String testCode,
            String testName,
            String unit,
            BigDecimal refRangeLow,
            BigDecimal refRangeHigh,
            String refRangeText,
            Integer sortOrder
    ) {}
}

