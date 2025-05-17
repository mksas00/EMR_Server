package service;

import entity.Allergy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.AllergyRepository;
import service.AllergyService;

import java.util.List;
import java.util.UUID;

@Service
public class AllergyServiceImpl implements AllergyService {

    @Autowired
    private AllergyRepository allergyRepository;

    @Override
    public List<Allergy> getAllergiesByPatientId(UUID patientId) {
        return allergyRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<Allergy> getAllergiesByAllergen(String allergen) {
        return allergyRepository.findByAllergen(allergen);
    }

    @Override
    public List<Allergy> getAllergiesBySeverity(String severity) {
        return allergyRepository.findBySeverity(severity);
    }

    @Override
    public Allergy saveAllergy(Allergy allergy) {
        return allergyRepository.save(allergy);
    }

    @Override
    public void deleteAllergy(UUID id) {
        allergyRepository.deleteById(id);
    }
}