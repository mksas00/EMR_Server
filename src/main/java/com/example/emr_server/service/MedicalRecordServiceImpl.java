package com.example.emr_server.service;

import com.example.emr_server.entity.MedicalRecord;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.MedicalRecordRepository;
import com.example.emr_server.controller.dto.MedicalRecordDto;
import com.example.emr_server.repository.spec.MedicalRecordSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository,
                                    AuthorizationService authorizationService,
                                    UserRepository userRepository,
                                    AuditService auditService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private MedicalRecordDto toDto(MedicalRecord r) {
        return new MedicalRecordDto(
                r.getId(),
                r.getPatient()!=null? r.getPatient().getId():null,
                r.getCreatedBy()!=null? r.getCreatedBy().getId():null,
                r.getRecordType(),
                r.getContent(),
                r.getIsEncrypted(),
                r.getEncryptedChecksum(),
                r.getCreatedAt()
        );
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByPatientId(UUID patientId) {
        User u = current();
        return medicalRecordRepository.findByPatient_Id(patientId).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByCreatedBy(UUID createdById) {
        User u = current();
        return medicalRecordRepository.findByCreatedBy_Id(createdById).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByType(String recordType) {
        User u = current();
        return medicalRecordRepository.findByRecordType(recordType).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByCreationRange(Instant startTimestamp, Instant endTimestamp) {
        User u = current();
        return medicalRecordRepository.findByCreatedAtBetween(startTimestamp, endTimestamp).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByPatientIdAndType(UUID patientId, String recordType) {
        User u = current();
        return medicalRecordRepository.findByPatient_IdAndRecordType(patientId, recordType).stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public MedicalRecord saveMedicalRecord(MedicalRecord medicalRecord) {
        User u = current();
        if (!authorizationService.canWritePatient(u, medicalRecord.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu rekordu medycznego");
        boolean create = medicalRecord.getId()==null;
        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_MEDICAL_RECORD":"UPDATE_MEDICAL_RECORD", "medRecordId="+saved.getId());
        return saved;
    }

    @Override
    public void deleteMedicalRecordById(UUID medicalRecordId) {
        medicalRecordRepository.findById(medicalRecordId).ifPresent(r -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, r.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia rekordu medycznego");
            medicalRecordRepository.delete(r);
            auditService.logPatient(u, r.getPatient(), "DELETE_MEDICAL_RECORD", "medRecordId="+r.getId());
        });
    }

    // Metody DTO + autoryzacja
    @Override
    public List<MedicalRecordDto> getVisibleByPatient(UUID patientId) {
        return getMedicalRecordsByPatientId(patientId).stream().map(this::toDto).toList();
    }

    @Override
    public List<MedicalRecordDto> getVisibleByCreatedBy(UUID createdById) {
        return getMedicalRecordsByCreatedBy(createdById).stream().map(this::toDto).toList();
    }

    @Override
    public List<MedicalRecordDto> getVisibleByType(String recordType) {
        return getMedicalRecordsByType(recordType).stream().map(this::toDto).toList();
    }

    @Override
    public List<MedicalRecordDto> getVisibleByCreationRange(Instant startTimestamp, Instant endTimestamp) {
        return getMedicalRecordsByCreationRange(startTimestamp, endTimestamp).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<MedicalRecordDto> getVisibleById(UUID id) {
        User u = current();
        return medicalRecordRepository.findById(id)
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .map(this::toDto);
    }

    @Override
    public MedicalRecordDto createForCurrent(MedicalRecord medicalRecord) {
        User u = current();
        if (!authorizationService.canWritePatient(u, medicalRecord.getPatient()))
            throw new SecurityException("Brak uprawnień do utworzenia rekordu medycznego");
        MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
        auditService.logPatient(u, saved.getPatient(), "CREATE_MEDICAL_RECORD", "medRecordId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<MedicalRecordDto> updateForCurrent(UUID id, MedicalRecord update) {
        User u = current();
        return medicalRecordRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji rekordu medycznego");
            existing.setRecordType(update.getRecordType());
            existing.setContent(update.getContent());
            existing.setIsEncrypted(update.getIsEncrypted());
            existing.setEncryptedChecksum(update.getEncryptedChecksum());
            MedicalRecord saved = medicalRecordRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_MEDICAL_RECORD", "medRecordId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        return medicalRecordRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia rekordu medycznego");
            medicalRecordRepository.delete(existing);
            auditService.logPatient(u, existing.getPatient(), "DELETE_MEDICAL_RECORD", "medRecordId="+existing.getId());
            return true;
        }).orElse(false);
    }

    @Override
    public Page<MedicalRecordDto> searchVisible(
            java.util.Optional<java.util.UUID> patientId,
            java.util.Optional<java.util.UUID> createdById,
            java.util.Optional<String> type,
            java.util.Optional<java.time.Instant> start,
            java.util.Optional<java.time.Instant> end,
            Pageable pageable
    ) {
        User u = current();
        var spec = MedicalRecordSpecifications.withFilters(patientId, createdById, type, start, end);
        var all = medicalRecordRepository.findAll(spec);
        var visible = all.stream()
                .filter(r -> authorizationService.canReadPatient(u, r.getPatient()))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }
}