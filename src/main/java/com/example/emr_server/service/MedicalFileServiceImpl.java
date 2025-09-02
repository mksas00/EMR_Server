package com.example.emr_server.service;

import com.example.emr_server.entity.MedicalFile;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.MedicalFileRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.controller.dto.MedicalFileDto;
import com.example.emr_server.repository.spec.MedicalFileSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MedicalFileServiceImpl implements MedicalFileService {

    private final MedicalFileRepository medicalFileRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public MedicalFileServiceImpl(MedicalFileRepository medicalFileRepository,
                                  AuthorizationService authorizationService,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.medicalFileRepository = medicalFileRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private MedicalFileDto toDto(MedicalFile f) {
        return new MedicalFileDto(
                f.getId(),
                f.getPatient()!=null? f.getPatient().getId():null,
                f.getUploadedBy()!=null? f.getUploadedBy().getId():null,
                f.getFileName(),
                f.getMimeType(),
                f.getUploadedAt()
        );
    }

    @Override
    public List<MedicalFile> getMedicalFilesByPatientId(UUID patientId) {
        User u = current();
        return medicalFileRepository.findByPatient_Id(patientId).stream()
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalFile> getMedicalFilesByUploadedBy(UUID uploadedById) {
        User u = current();
        return medicalFileRepository.findByUploadedBy_Id(uploadedById).stream()
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalFile> getMedicalFilesByMimeType(String mimeType) {
        User u = current();
        return medicalFileRepository.findByMimeType(mimeType).stream()
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalFile> getMedicalFilesByUploadedAtRange(Instant startTimestamp, Instant endTimestamp) {
        User u = current();
        return medicalFileRepository.findByUploadedAtBetween(startTimestamp, endTimestamp).stream()
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalFile> getMedicalFilesByPatientIdAndMimeType(UUID patientId, String mimeType) {
        User u = current();
        return medicalFileRepository.findByPatient_IdAndMimeType(patientId, mimeType).stream()
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalFile> getMedicalFilesByFileNameFragment(String fileNameFragment) {
        User u = current();
        return medicalFileRepository.findByFileNameContaining(fileNameFragment).stream()
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public MedicalFile saveMedicalFile(MedicalFile medicalFile) {
        User u = current();
        if (!authorizationService.canWritePatient(u, medicalFile.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu pliku medycznego");
        boolean create = medicalFile.getId()==null;
        MedicalFile saved = medicalFileRepository.save(medicalFile);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_MEDICAL_FILE":"UPDATE_MEDICAL_FILE", "fileId="+saved.getId());
        return saved;
    }

    @Override
    public void deleteMedicalFileById(UUID medicalFileId) {
        medicalFileRepository.findById(medicalFileId).ifPresent(f -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, f.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia pliku medycznego");
            medicalFileRepository.delete(f);
            auditService.logPatient(u, f.getPatient(), "DELETE_MEDICAL_FILE", "fileId="+f.getId());
        });
    }

    // Metody DTO + autoryzacja
    @Override
    public List<MedicalFileDto> getVisibleByPatient(UUID patientId) {
        return getMedicalFilesByPatientId(patientId).stream().map(this::toDto).toList();
    }

    @Override
    public List<MedicalFileDto> getVisibleByUploadedBy(UUID uploadedById) {
        return getMedicalFilesByUploadedBy(uploadedById).stream().map(this::toDto).toList();
    }

    @Override
    public List<MedicalFileDto> getVisibleByMimeType(String mimeType) {
        return getMedicalFilesByMimeType(mimeType).stream().map(this::toDto).toList();
    }

    @Override
    public List<MedicalFileDto> getVisibleByUploadedAtRange(Instant startTimestamp, Instant endTimestamp) {
        return getMedicalFilesByUploadedAtRange(startTimestamp, endTimestamp).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<MedicalFileDto> getVisibleById(UUID id) {
        User u = current();
        return medicalFileRepository.findById(id)
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .map(this::toDto);
    }

    @Override
    public MedicalFileDto createForCurrent(MedicalFile file) {
        User u = current();
        if (!authorizationService.canWritePatient(u, file.getPatient()))
            throw new SecurityException("Brak uprawnień do utworzenia pliku medycznego");
        MedicalFile saved = medicalFileRepository.save(file);
        auditService.logPatient(u, saved.getPatient(), "CREATE_MEDICAL_FILE", "fileId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<MedicalFileDto> updateForCurrent(UUID id, MedicalFile update) {
        User u = current();
        return medicalFileRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji pliku medycznego");
            existing.setFileName(update.getFileName());
            existing.setFilePath(update.getFilePath());
            existing.setMimeType(update.getMimeType());
            MedicalFile saved = medicalFileRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_MEDICAL_FILE", "fileId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        return medicalFileRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia pliku medycznego");
            medicalFileRepository.delete(existing);
            auditService.logPatient(u, existing.getPatient(), "DELETE_MEDICAL_FILE", "fileId="+existing.getId());
            return true;
        }).orElse(false);
    }

    @Override
    public Page<MedicalFileDto> searchVisible(
            java.util.Optional<java.util.UUID> patientId,
            java.util.Optional<java.util.UUID> uploadedById,
            java.util.Optional<String> mimeType,
            java.util.Optional<java.time.Instant> start,
            java.util.Optional<java.time.Instant> end,
            java.util.Optional<String> fileNameFragment,
            Pageable pageable
    ) {
        User u = current();
        var spec = MedicalFileSpecifications.withFilters(patientId, uploadedById, mimeType, start, end, fileNameFragment);
        var all = medicalFileRepository.findAll(spec);
        var visible = all.stream()
                .filter(f -> authorizationService.canReadPatient(u, f.getPatient()))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }
}