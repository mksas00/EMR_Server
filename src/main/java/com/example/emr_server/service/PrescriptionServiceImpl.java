package com.example.emr_server.service;

import com.example.emr_server.entity.Prescription;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.PrescriptionRepository;
import com.example.emr_server.controller.dto.PrescriptionDto;
import com.example.emr_server.controller.dto.PrescriptionItemDto;
import com.example.emr_server.entity.PrescriptionMedication;
import com.example.emr_server.repository.spec.PrescriptionSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   AuthorizationService authorizationService,
                                   UserRepository userRepository,
                                   AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private PrescriptionItemDto toItemDto(PrescriptionMedication it) {
        return new PrescriptionItemDto(
                it.getId(),
                it.getMedication()!=null? it.getMedication().getId(): null,
                it.getDosageInfo(),
                it.getQuantity(),
                it.getUnit()
        );
    }

    private PrescriptionDto toDto(Prescription p) {
        List<PrescriptionItemDto> items = p.getItems()!=null? p.getItems().stream().map(this::toItemDto).toList(): List.of();
        return new PrescriptionDto(
                p.getId(),
                p.getPatient() != null ? p.getPatient().getId() : null,
                p.getDoctor() != null ? p.getDoctor().getId() : null,
                p.getDosageInfo(),
                p.getIssuedDate(),
                p.getExpirationDate(),
                p.getIsRepeatable(),
                items
        );
    }

    @Override
    public List<Prescription> getPrescriptionsByPatientId(UUID patientId) {
        User u = current();
        return prescriptionRepository.findByPatient_Id(patientId).stream()
                .filter(p -> authorizationService.canReadPatient(u, p.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Prescription> getPrescriptionsByDoctorId(UUID doctorId) {
        User u = current();
        return prescriptionRepository.findByDoctor_Id(doctorId).stream()
                .filter(p -> {
                    if (u == null) return false;
                    if (u.getId().equals(doctorId)) return true; // własne
                    return authorizationService.canReadPatient(u, p.getPatient());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Prescription> getActivePrescriptions(LocalDate currentDate) {
        User u = current();
        return prescriptionRepository.findByExpirationDateAfter(currentDate).stream()
                .filter(p -> authorizationService.canReadPatient(u, p.getPatient()))
                .collect(Collectors.toList());
    }


    @Override
    public List<Prescription> getPrescriptionsByIssuedDateRange(LocalDate start, LocalDate end) {
        User u = current();
        return prescriptionRepository.findByIssuedDateBetween(start, end).stream()
                .filter(p -> authorizationService.canReadPatient(u, p.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public Prescription savePrescription(Prescription prescription) {
        User u = current();
        if (!authorizationService.canWritePatient(u, prescription.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu recepty");
        boolean create = prescription.getId()==null;
        Prescription saved = prescriptionRepository.save(prescription);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_PRESCRIPTION":"UPDATE_PRESCRIPTION", "prescriptionId="+saved.getId());
        return saved;
    }

    @Override
    public void deletePrescriptionById(UUID prescriptionId) {
        prescriptionRepository.findById(prescriptionId).ifPresent(p -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, p.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia recepty");
            prescriptionRepository.delete(p);
            auditService.logPatient(u, p.getPatient(), "DELETE_PRESCRIPTION", "prescriptionId="+p.getId());
        });
    }

    // Metody DTO + autoryzacja
    @Override
    public List<PrescriptionDto> getVisibleByPatient(UUID patientId) {
        return getPrescriptionsByPatientId(patientId).stream().map(this::toDto).toList();
    }

    @Override
    public List<PrescriptionDto> getVisibleByDoctor(UUID doctorId) {
        return getPrescriptionsByDoctorId(doctorId).stream().map(this::toDto).toList();
    }

    @Override
    public List<PrescriptionDto> getVisibleActive(LocalDate now) {
        return getActivePrescriptions(now).stream().map(this::toDto).toList();
    }

    @Override
    public List<PrescriptionDto> getVisibleByIssuedDateRange(LocalDate start, LocalDate end) {
        return getPrescriptionsByIssuedDateRange(start, end).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<PrescriptionDto> getVisibleById(UUID id) {
        User u = current();
        return prescriptionRepository.findById(id)
                .filter(p -> authorizationService.canReadPatient(u, p.getPatient()))
                .map(this::toDto);
    }

    @Override
    public PrescriptionDto createForCurrent(Prescription prescription) {
        User u = current();
        if (!authorizationService.canWritePatient(u, prescription.getPatient()))
            throw new SecurityException("Brak uprawnień do utworzenia recepty");
        // enforce doctor and defaults
        prescription.setDoctor(u);
        if (prescription.getIssuedDate() == null) {
            prescription.setIssuedDate(LocalDate.now());
        }
        if (prescription.getExpirationDate() == null && prescription.getIssuedDate() != null) {
            prescription.setExpirationDate(prescription.getIssuedDate().plus(30, ChronoUnit.DAYS));
        }
        Prescription saved = prescriptionRepository.save(prescription);
        auditService.logPatient(u, saved.getPatient(), "CREATE_PRESCRIPTION", "prescriptionId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<PrescriptionDto> updateForCurrent(UUID id, Prescription update) {
        User u = current();
        return prescriptionRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji recepty");
            existing.setDosageInfo(update.getDosageInfo());
            existing.setIssuedDate(update.getIssuedDate());
            existing.setExpirationDate(update.getExpirationDate());
            existing.setIsRepeatable(update.getIsRepeatable());
            if (update.getItems() != null) {
                existing.setItems(update.getItems()); // orphanRemoval zadba o różnice
            }
            Prescription saved = prescriptionRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_PRESCRIPTION", "prescriptionId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        return prescriptionRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia recepty");
            prescriptionRepository.delete(existing);
            auditService.logPatient(u, existing.getPatient(), "DELETE_PRESCRIPTION", "prescriptionId="+existing.getId());
            return true;
        }).orElse(false);
    }

    @Override
    public Page<PrescriptionDto> searchVisible(
            java.util.Optional<java.util.UUID> patientId,
            java.util.Optional<java.util.UUID> doctorId,
            java.util.Optional<java.lang.Boolean> active,
            java.util.Optional<java.time.LocalDate> start,
            java.util.Optional<java.time.LocalDate> end,
            Pageable pageable
    ) {
        User u = current();
        var spec = PrescriptionSpecifications.withFilters(patientId, doctorId, active, start, end);
        var all = prescriptionRepository.findAll(spec);
        var visible = all.stream()
                .filter(p -> authorizationService.canReadPatient(u, p.getPatient()))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }
}