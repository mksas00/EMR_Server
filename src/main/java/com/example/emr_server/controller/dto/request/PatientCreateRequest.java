package com.example.emr_server.controller.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Map;

public record PatientCreateRequest(
        @NotBlank @Size(max = 300) String firstName,
        @NotBlank @Size(max = 300) String lastName,
        @NotBlank @Pattern(regexp = "\\d{11}") String pesel,
        @NotNull @PastOrPresent LocalDate dateOfBirth,
        @Size(max = 10) String gender,
        Map<String, Object> contactInfo,
        @Size(max = 10000) String address
) {}

