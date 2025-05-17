package service;

import entity.ChronicDisease;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.ChronicDiseaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ChronicDiseaseServiceImpl implements ChronicDiseaseService {

    @Autowired
    private ChronicDiseaseRepository chronicDiseaseRepository;

    @Override
    public List<ChronicDisease> getDiseasesByPatientId(UUID patientId) {
        return chronicDiseaseRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<ChronicDisease> getDiseasesByName(String diseaseName) {
        return chronicDiseaseRepository.findByDiseaseName(diseaseName);
    }

    @Override
    public List<ChronicDisease> getDiseasesByDiagnosedDates(LocalDate startDate, LocalDate endDate) {
        return chronicDiseaseRepository.findByDiagnosedDateBetween(startDate, endDate);
    }

    @Override
    public List<ChronicDisease> getDiseasesByNotesFragment(String notesFragment) {
        return chronicDiseaseRepository.findByNotesContaining(notesFragment);
    }

    @Override
    public ChronicDisease saveDisease(ChronicDisease disease) {
        return chronicDiseaseRepository.save(disease);
    }

    @Override
    public void deleteDiseaseById(UUID diseaseId) {
        chronicDiseaseRepository.deleteById(diseaseId);
    }
}