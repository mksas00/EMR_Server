package com.example.emr_server.service;

import com.example.emr_server.entity.LabResult;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.LabResultRepository;
import com.example.emr_server.controller.dto.LabResultDto;
import com.example.emr_server.repository.spec.LabResultSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LabResultServiceImpl implements LabResultService {

    private final LabResultRepository labResultRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public LabResultServiceImpl(LabResultRepository labResultRepository,
                                AuthorizationService authorizationService,
                                UserRepository userRepository,
                                AuditService auditService) {
        this.labResultRepository = labResultRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private LabResultDto toDto(LabResult r) {
        return new LabResultDto(
                r.getId(),
                r.getPatient()!=null? r.getPatient().getId():null,
                r.getOrderedBy()!=null? r.getOrderedBy().getId():null,
                r.getTestName(),
                r.getResult(),
                r.getResultDate(),
                r.getUnit(),
                r.getReferenceRange(),
                r.getStatus()
        );
    }

    @Override
    public List<LabResult> getLabResultsByPatientId(UUID patientId) {
        User u = current();
        return labResultRepository.findByPatient_Id(patientId).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LabResult> getLabResultsByOrderedBy(UUID orderedById) {
        User u = current();
        return labResultRepository.findByOrderedBy_Id(orderedById).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LabResult> getLabResultsByTestName(String testName) {
        User u = current();
        return labResultRepository.findByTestName(testName).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LabResult> getLabResultsByDateRange(LocalDate startDate, LocalDate endDate) {
        User u = current();
        return labResultRepository.findByResultDateBetween(startDate, endDate).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LabResult> getLabResultsByStatus(String status) {
        User u = current();
        return labResultRepository.findByStatus(status).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LabResult> getLabResultsByResultFragment(String resultFragment) {
        User u = current();
        return labResultRepository.findByResultContaining(resultFragment).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public LabResult saveLabResult(LabResult labResult) {
        User u = current();
        if (!authorizationService.canWritePatient(u, labResult.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu wyniku lab.");
        boolean create = labResult.getId() == null;
        LabResult saved = labResultRepository.save(labResult);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_LAB_RESULT":"UPDATE_LAB_RESULT", "labResultId="+saved.getId());
        return saved;
    }

    @Override
    public void deleteLabResultById(UUID labResultId) {
        labResultRepository.findById(labResultId).ifPresent(r -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, r.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia wyniku lab.");
            labResultRepository.delete(r);
            auditService.logPatient(u, r.getPatient(), "DELETE_LAB_RESULT", "labResultId="+r.getId());
        });
    }

    // Metody DTO + autoryzacja
    @Override
    public List<LabResultDto> getVisibleByPatient(UUID patientId) {
        return getLabResultsByPatientId(patientId).stream().map(this::toDto).toList();
    }

    @Override
    public List<LabResultDto> getVisibleByOrderedBy(UUID orderedById) {
        return getLabResultsByOrderedBy(orderedById).stream().map(this::toDto).toList();
    }

    @Override
    public List<LabResultDto> getVisibleByStatus(String status) {
        return getLabResultsByStatus(status).stream().map(this::toDto).toList();
    }

    @Override
    public List<LabResultDto> getVisibleByDateRange(LocalDate startDate, LocalDate endDate) {
        return getLabResultsByDateRange(startDate, endDate).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<LabResultDto> getVisibleById(UUID id) {
        User u = current();
        return labResultRepository.findById(id)
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .map(this::toDto);
    }

    @Override
    public LabResultDto createForCurrent(LabResult labResult) {
        User u = current();
        if (!authorizationService.canWritePatient(u, labResult.getPatient()))
            throw new SecurityException("Brak uprawnień do utworzenia wyniku lab.");
        LabResult saved = labResultRepository.save(labResult);
        auditService.logPatient(u, saved.getPatient(), "CREATE_LAB_RESULT", "labResultId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<LabResultDto> updateForCurrent(UUID id, LabResult update) {
        User u = current();
        return labResultRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji wyniku lab.");
            existing.setTestName(update.getTestName());
            existing.setResult(update.getResult());
            existing.setResultDate(update.getResultDate());
            existing.setUnit(update.getUnit());
            existing.setReferenceRange(update.getReferenceRange());
            existing.setStatus(update.getStatus());
            LabResult saved = labResultRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_LAB_RESULT", "labResultId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        return labResultRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia wyniku lab.");
            labResultRepository.delete(existing);
            auditService.logPatient(u, existing.getPatient(), "DELETE_LAB_RESULT", "labResultId="+existing.getId());
            return true;
        }).orElse(false);
    }

    @Override
    public Page<LabResultDto> searchVisible(
            java.util.Optional<java.util.UUID> patientId,
            java.util.Optional<java.util.UUID> orderedById,
            java.util.Optional<String> status,
            java.util.Optional<java.time.LocalDate> start,
            java.util.Optional<java.time.LocalDate> end,
            java.util.Optional<String> testName,
            java.util.Optional<String> resultFragment,
            Pageable pageable
    ) {
        User u = current();
        var spec = LabResultSpecifications.withFilters(patientId, orderedById, status, start, end, testName, resultFragment);
        var all = labResultRepository.findAll(spec);
        var visible = all.stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }
}