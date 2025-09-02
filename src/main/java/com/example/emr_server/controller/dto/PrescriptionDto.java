package com.example.emr_server.controller.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PrescriptionDto(
        UUID id,
        UUID patientId,
        UUID doctorId,
        String dosageInfo,
        LocalDate issuedDate,
        LocalDate expirationDate,
        Boolean isRepeatable,
        List<PrescriptionItemDto> items
) {}
