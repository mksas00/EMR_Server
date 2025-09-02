package com.example.emr_server.controller.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record PatientDto(
        UUID id,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        Map<String, Object> contactInfo,
        String address
) {}
