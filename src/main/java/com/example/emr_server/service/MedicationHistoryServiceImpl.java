package com.example.emr_server.service;

import com.example.emr_server.entity.MedicationHistory;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.MedicationHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MedicationHistoryServiceImpl implements MedicationHistoryService {

    private final MedicationHistoryRepository medicationHistoryRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public MedicationHistoryServiceImpl(MedicationHistoryRepository medicationHistoryRepository,
                                        AuthorizationService authorizationService,
                                        UserRepository userRepository,
                                        AuditService auditService) {
        this.medicationHistoryRepository = medicationHistoryRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    @Override
    public List<MedicationHistory> getMedicationHistoryByPatientId(UUID patientId) {
        User u = current();
        return medicationHistoryRepository.findByPatient_Id(patientId).stream()
                .filter(m -> authorizationService.canReadPatient(u, m.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicationHistory> getActiveMedicationsOnDate(LocalDate date) {
        User u = current();
        return medicationHistoryRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrEndDateIsNull(date, date).stream()
                .filter(m -> authorizationService.canReadPatient(u, m.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicationHistory> getMedicationHistoryByPatientIdAndDateRange(UUID patientId, LocalDate startDate, LocalDate endDate) {
        User u = current();
        return medicationHistoryRepository.findByPatient_IdAndStartDateBetween(patientId, startDate, endDate).stream()
                .filter(m -> authorizationService.canReadPatient(u, m.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicationHistory> getMedicationHistoryByReason(String reason) {
        throw new UnsupportedOperationException("Wyszukiwanie po fragmencie reason niedostępne (losowe szyfrowanie)");
    }

    @Override
    public List<MedicationHistory> getMedicationHistoryByPatientIdAndMedicationId(UUID patientId, UUID medicationId) {
        User u = current();
        return medicationHistoryRepository.findByPatient_IdAndMedicationRef_Id(patientId, medicationId).stream()
                .filter(m -> authorizationService.canReadPatient(u, m.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public MedicationHistory saveMedicationHistory(MedicationHistory medicationHistory) {
        User u = current();
        if (!authorizationService.canWritePatient(u, medicationHistory.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu historii leków");
        boolean create = medicationHistory.getId()==null;
        MedicationHistory saved = medicationHistoryRepository.save(medicationHistory);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_MEDICATION_HISTORY":"UPDATE_MEDICATION_HISTORY", "medHistId="+saved.getId());
        return saved;
    }

    @Override
    public void deleteMedicationHistoryById(UUID medicationHistoryId) {
        medicationHistoryRepository.findById(medicationHistoryId).ifPresent(m -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, m.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia historii leków");
            medicationHistoryRepository.delete(m);
            auditService.logPatient(u, m.getPatient(), "DELETE_MEDICATION_HISTORY", "medHistId="+m.getId());
        });
    }
}