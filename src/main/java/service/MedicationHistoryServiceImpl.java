package service;

import entity.MedicationHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.MedicationHistoryRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MedicationHistoryServiceImpl implements MedicationHistoryService {

    @Autowired
    private MedicationHistoryRepository medicationHistoryRepository;

    @Override
    public List<MedicationHistory> getMedicationHistoryByPatientId(UUID patientId) {
        return medicationHistoryRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<MedicationHistory> getActiveMedicationsOnDate(LocalDate date) {
        return medicationHistoryRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrEndDateIsNull(date, date);
    }

    @Override
    public List<MedicationHistory> getMedicationHistoryByPatientIdAndDateRange(UUID patientId, LocalDate startDate, LocalDate endDate) {
        return medicationHistoryRepository.findByPatient_IdAndStartDateBetween(patientId, startDate, endDate);
    }

    @Override
    public List<MedicationHistory> getMedicationHistoryByReason(String reason) {
        return medicationHistoryRepository.findByReasonContaining(reason);
    }

    @Override
    public List<MedicationHistory> getMedicationHistoryByPatientIdAndMedication(UUID patientId, String medication) {
        return medicationHistoryRepository.findByPatient_IdAndMedication(patientId, medication);
    }

    @Override
    public MedicationHistory saveMedicationHistory(MedicationHistory medicationHistory) {
        return medicationHistoryRepository.save(medicationHistory);
    }

    @Override
    public void deleteMedicationHistoryById(UUID medicationHistoryId) {
        medicationHistoryRepository.deleteById(medicationHistoryId);
    }
}