package com.example.emr_server.service;

import com.example.emr_server.entity.Allergy;
import com.example.emr_server.controller.dto.AllergyDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AllergyService {
    List<Allergy> getAllergiesByPatientId(UUID patientId);
    List<Allergy> getAllergiesByAllergen(String allergen);
    List<Allergy> getAllergiesBySeverity(String severity);
    Allergy saveAllergy(Allergy allergy);
    void deleteAllergy(UUID id);

    // Nowe metody DTO + autoryzacja
    List<AllergyDto> getVisibleByPatient(UUID patientId);
    List<AllergyDto> getVisibleByAllergen(String allergen);
    List<AllergyDto> getVisibleBySeverity(String severity);
    Optional<AllergyDto> getVisibleById(UUID id);
    AllergyDto createForCurrent(Allergy allergy);
    Optional<AllergyDto> updateForCurrent(UUID id, Allergy update);
    boolean deleteForCurrent(UUID id);

    // Nowe: filtrowanie AND + paginacja/sort
    Page<AllergyDto> searchVisible(
            Optional<UUID> patientId,
            Optional<String> allergen,
            Optional<String> severity,
            Optional<UUID> notedById,
            Pageable pageable
    );
}