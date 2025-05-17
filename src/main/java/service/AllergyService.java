package service;

import entity.Allergy;

import java.util.List;
import java.util.UUID;

public interface AllergyService {
    List<Allergy> getAllergiesByPatientId(UUID patientId);
    List<Allergy> getAllergiesByAllergen(String allergen);
    List<Allergy> getAllergiesBySeverity(String severity);
    Allergy saveAllergy(Allergy allergy);
    void deleteAllergy(UUID id);
}