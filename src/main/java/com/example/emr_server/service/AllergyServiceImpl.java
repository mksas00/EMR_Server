package com.example.emr_server.service;

import com.example.emr_server.entity.Allergy;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.AllergyRepository;
import com.example.emr_server.controller.dto.AllergyDto;
import com.example.emr_server.repository.spec.AllergySpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AllergyServiceImpl implements AllergyService {

    private final AllergyRepository allergyRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AllergyServiceImpl(AllergyRepository allergyRepository,
                              AuthorizationService authorizationService,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.allergyRepository = allergyRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private AllergyDto toDto(Allergy a) {
        return new AllergyDto(
                a.getId(),
                a.getPatient()!=null? a.getPatient().getId():null,
                a.getNotedBy()!=null? a.getNotedBy().getId():null,
                a.getAllergen(),
                a.getReaction(),
                a.getSeverity()
        );
    }

    @Override
    public List<Allergy> getAllergiesByPatientId(UUID patientId) {
        User u = current();
        return allergyRepository.findByPatient_Id(patientId).stream()
                .filter(a -> authorizationService.canReadPatient(u, a.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Allergy> getAllergiesByAllergen(String allergen) {
        User u = current();
        return allergyRepository.findByAllergen(allergen).stream()
                .filter(a -> authorizationService.canReadPatient(u, a.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Allergy> getAllergiesBySeverity(String severity) {
        User u = current();
        return allergyRepository.findBySeverity(severity).stream()
                .filter(a -> authorizationService.canReadPatient(u, a.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public Allergy saveAllergy(Allergy allergy) {
        User u = current();
        if (!authorizationService.canWritePatient(u, allergy.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu alergii pacjenta");
        boolean create = allergy.getId()==null;
        Allergy saved = allergyRepository.save(allergy);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_ALLERGY":"UPDATE_ALLERGY", "allergyId="+saved.getId());
        return saved;
    }

    @Override
    public void deleteAllergy(UUID id) {
        allergyRepository.findById(id).ifPresent(a -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, a.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia alergii");
            allergyRepository.delete(a);
            auditService.logPatient(u, a.getPatient(), "DELETE_ALLERGY", "allergyId="+a.getId());
        });
    }

    // Metody DTO + autoryzacja
    @Override
    public List<AllergyDto> getVisibleByPatient(UUID patientId) {
        return getAllergiesByPatientId(patientId).stream().map(this::toDto).toList();
    }

    @Override
    public List<AllergyDto> getVisibleByAllergen(String allergen) {
        return getAllergiesByAllergen(allergen).stream().map(this::toDto).toList();
    }

    @Override
    public List<AllergyDto> getVisibleBySeverity(String severity) {
        return getAllergiesBySeverity(severity).stream().map(this::toDto).toList();
    }

    @Override
    public Optional<AllergyDto> getVisibleById(UUID id) {
        User u = current();
        return allergyRepository.findById(id)
                .filter(a -> authorizationService.canReadPatient(u, a.getPatient()))
                .map(this::toDto);
    }

    @Override
    public AllergyDto createForCurrent(Allergy allergy) {
        User u = current();
        if (!authorizationService.canWritePatient(u, allergy.getPatient()))
            throw new SecurityException("Brak uprawnień do utworzenia alergii");
        Allergy saved = allergyRepository.save(allergy);
        auditService.logPatient(u, saved.getPatient(), "CREATE_ALLERGY", "allergyId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<AllergyDto> updateForCurrent(UUID id, Allergy update) {
        User u = current();
        return allergyRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji alergii");
            existing.setAllergen(update.getAllergen());
            existing.setReaction(update.getReaction());
            existing.setSeverity(update.getSeverity());
            Allergy saved = allergyRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_ALLERGY", "allergyId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        return allergyRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia alergii");
            allergyRepository.delete(existing);
            auditService.logPatient(u, existing.getPatient(), "DELETE_ALLERGY", "allergyId="+existing.getId());
            return true;
        }).orElse(false);
    }

    @Override
    public Page<AllergyDto> searchVisible(
            java.util.Optional<java.util.UUID> patientId,
            java.util.Optional<String> allergen,
            java.util.Optional<String> severity,
            java.util.Optional<java.util.UUID> notedById,
            Pageable pageable
    ) {
        User u = current();
        var spec = AllergySpecifications.withFilters(patientId, allergen, severity, notedById);
        var all = allergyRepository.findAll(spec);
        var visible = all.stream()
                .filter(a -> authorizationService.canReadPatient(u, a.getPatient()))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }
}