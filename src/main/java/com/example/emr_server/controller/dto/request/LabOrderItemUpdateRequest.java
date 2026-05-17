package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Wprowadzenie / aktualizacja wyniku pozycji zlecenia.
 * Pola opcjonalne – można wypełnić częściowo (np. tylko notes).
 * Status zmieniany niezależnie przez endpoint /complete lub /cancel.
 */
public record LabOrderItemUpdateRequest(
        @Size(max = 2000) String result,
        BigDecimal resultNumeric,
        @Size(max = 20) String unit,
        BigDecimal refRangeLow,
        BigDecimal refRangeHigh,
        @Size(max = 200) String refRangeText,
        @Size(max = 2000) String notes,
        @Pattern(regexp = "L|H|N|LL|HH", message = "abnormalFlag must be L|H|N|LL|HH")
        String abnormalFlag,
        @Pattern(regexp = "pending|completed|cancelled", message = "status must be pending|completed|cancelled")
        String status
) {}

