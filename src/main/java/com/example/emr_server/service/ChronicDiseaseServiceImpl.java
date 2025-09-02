package com.example.emr_server.service;

import com.example.emr_server.entity.ChronicDisease;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.ChronicDiseaseRepository;
import com.example.emr_server.controller.dto.ChronicDiseaseDto;
import com.example.emr_server.repository.spec.ChronicDiseaseSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChronicDiseaseServiceImpl implements ChronicDiseaseService {

    private final ChronicDiseaseRepository chronicDiseaseRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ChronicDiseaseServiceImpl(ChronicDiseaseRepository chronicDiseaseRepository,
                                     AuthorizationService authorizationService,
                                     UserRepository userRepository,
                                     AuditService auditService) {
        this.chronicDiseaseRepository = chronicDiseaseRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private ChronicDiseaseDto toDto(ChronicDisease cd) {
        return new ChronicDiseaseDto(
                cd.getId(),
                cd.getPatient()!=null? cd.getPatient().getId():null,
                cd.getDiseaseName(),
                cd.getDiagnosedDate(),
                cd.getNotes()
        );
    }

    @Override
    public List<ChronicDisease> getDiseasesByPatientId(UUID patientId) {
        User u = current();
        return chronicDiseaseRepository.findByPatient_Id(patientId).stream()
                .filter(cd -> authorizationService.canReadPatient(u, cd.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChronicDisease> getDiseasesByName(String diseaseName) {
        User u = current();
        return chronicDiseaseRepository.findByDiseaseName(diseaseName).stream()
                .filter(cd -> authorizationService.canReadPatient(u, cd.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChronicDisease> getDiseasesByDiagnosedDates(LocalDate startDate, LocalDate endDate) {
        User u = current();
        return chronicDiseaseRepository.findByDiagnosedDateBetween(startDate, endDate).stream()
                .filter(cd -> authorizationService.canReadPatient(u, cd.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChronicDisease> getDiseasesByNotesFragment(String notesFragment) {
        User u = current();
        return chronicDiseaseRepository.findByNotesContaining(notesFragment).stream()
                .filter(cd -> authorizationService.canReadPatient(u, cd.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public ChronicDisease saveDisease(ChronicDisease disease) {
        User u = current();
        if (!authorizationService.canWritePatient(u, disease.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu choroby przewlekłej");
        boolean create = disease.getId() == null;
        ChronicDisease saved = chronicDiseaseRepository.save(disease);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_CHRONIC_DISEASE":"UPDATE_CHRONIC_DISEASE", "diseaseId="+saved.getId());
        return saved;
    }

    @Override
    public void deleteDiseaseById(UUID diseaseId) {
        chronicDiseaseRepository.findById(diseaseId).ifPresent(cd -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, cd.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia choroby");
            chronicDiseaseRepository.delete(cd);
            auditService.logPatient(u, cd.getPatient(), "DELETE_CHRONIC_DISEASE", "diseaseId="+cd.getId());
        });
    }

    // Metody DTO + autoryzacja
    @Override
    public List<ChronicDiseaseDto> getVisibleByPatient(UUID patientId) {
        return getDiseasesByPatientId(patientId).stream().map(this::toDto).toList();
    }

    @Override
    public List<ChronicDiseaseDto> getVisibleByName(String diseaseName) {
        return getDiseasesByName(diseaseName).stream().map(this::toDto).toList();
    }

    @Override
    public List<ChronicDiseaseDto> getVisibleByDiagnosedDates(LocalDate startDate, LocalDate endDate) {
        return getDiseasesByDiagnosedDates(startDate, endDate).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<ChronicDiseaseDto> getVisibleById(UUID id) {
        User u = current();
        return chronicDiseaseRepository.findById(id)
                .filter(cd -> authorizationService.canReadPatient(u, cd.getPatient()))
                .map(this::toDto);
    }

    @Override
    public ChronicDiseaseDto createForCurrent(ChronicDisease disease) {
        User u = current();
        if (!authorizationService.canWritePatient(u, disease.getPatient()))
            throw new SecurityException("Brak uprawnień do utworzenia choroby przewlekłej");
        ChronicDisease saved = chronicDiseaseRepository.save(disease);
        auditService.logPatient(u, saved.getPatient(), "CREATE_CHRONIC_DISEASE", "diseaseId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<ChronicDiseaseDto> updateForCurrent(UUID id, ChronicDisease update) {
        User u = current();
        return chronicDiseaseRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji choroby przewlekłej");
            existing.setDiseaseName(update.getDiseaseName());
            existing.setDiagnosedDate(update.getDiagnosedDate());
            existing.setNotes(update.getNotes());
            ChronicDisease saved = chronicDiseaseRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_CHRONIC_DISEASE", "diseaseId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        return chronicDiseaseRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia choroby przewlekłej");
            chronicDiseaseRepository.delete(existing);
            auditService.logPatient(u, existing.getPatient(), "DELETE_CHRONIC_DISEASE", "diseaseId="+existing.getId());
            return true;
        }).orElse(false);
    }

    @Override
    public Page<ChronicDiseaseDto> searchVisible(
            java.util.Optional<java.util.UUID> patientId,
            java.util.Optional<String> diseaseName,
            java.util.Optional<java.time.LocalDate> start,
            java.util.Optional<java.time.LocalDate> end,
            java.util.Optional<String> notesFragment,
            Pageable pageable
    ) {
        User u = current();
        var spec = ChronicDiseaseSpecifications.withFilters(patientId, diseaseName, start, end, notesFragment);
        var all = chronicDiseaseRepository.findAll(spec);
        var visible = all.stream()
                .filter(cd -> authorizationService.canReadPatient(u, cd.getPatient()))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }
}