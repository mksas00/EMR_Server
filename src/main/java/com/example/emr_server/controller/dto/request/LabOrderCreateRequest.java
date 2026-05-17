package com.example.emr_server.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Tworzenie zlecenia laboratoryjnego.
 * Dwa źródła pozycji: panelIds (predefiniowane szablony) + items (ręczne).
 * Co najmniej jedno ze źródeł musi dać >0 pozycji.
 */
public record LabOrderCreateRequest(
        @NotNull UUID patientId,
        @Pattern(regexp = "routine|urgent|stat", message = "priority must be routine|urgent|stat")
        String priority,
        @Size(max = 2000) String notes,
        List<@NotNull UUID> panelIds,
        @Valid List<Item> items
) {
    public record Item(
            @NotNull @Size(min = 1, max = 40) String testCode,
            @NotNull @Size(min = 1, max = 200) String testName,
            @Size(max = 20) String unit,
            BigDecimal refRangeLow,
            BigDecimal refRangeHigh,
            @Size(max = 200) String refRangeText
    ) {}
}

