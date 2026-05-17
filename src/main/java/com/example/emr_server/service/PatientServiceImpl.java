package com.example.emr_server.service;

import com.example.emr_server.entity.Patient;
import com.example.emr_server.repository.PatientRepository;
import com.example.emr_server.security.encryption.EncryptionService;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.entity.User;
import com.example.emr_server.controller.dto.PatientDto;
import com.example.emr_server.repository.spec.PatientSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final EncryptionService encryptionService;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PatientServiceImpl(PatientRepository patientRepository,
                              EncryptionService encryptionService,
                              AuthorizationService authorizationService,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.patientRepository = patientRepository;
        this.encryptionService = encryptionService;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private String encDet(String logicalField, String value) {
        if (value == null || value.isBlank()) return value;
        return encryptionService.encryptDeterministic(logicalField, value.trim());
    }

    private User current() {
        return SecurityUtil.getCurrentUser(userRepository).orElse(null);
    }

    private PatientDto toDto(Patient p) {
        if (p == null) return null;
        return new PatientDto(
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getPesel(),
                p.getDateOfBirth(),
                p.getGender(),
                p.getContactInfo(),
                p.getAddress()
        );
    }

    @Override
    public List<Patient> getPatientsByFirstName(String firstName) {
        return patientRepository.findByFirstName(encDet("patient.first_name", firstName));
    }

    @Override
    public List<Patient> getPatientsByLastName(String lastName) {
        return patientRepository.findByLastName(encDet("patient.last_name", lastName));
    }

    @Override
    public List<Patient> getPatientsByFirstNameAndLastName(String firstName, String lastName) {
        return patientRepository.findByFirstNameAndLastName(
                encDet("patient.first_name", firstName),
                encDet("patient.last_name", lastName)
        );
    }

    @Override
    public Optional<Patient> getPatientByPesel(String pesel) {
        return patientRepository.findByPesel(encDet("patient.pesel", pesel));
    }

    @Override
    public List<Patient> getPatientsByDateOfBirthBetween(LocalDate startDate, LocalDate endDate) {
        return patientRepository.findByDateOfBirthBetween(startDate, endDate);
    }

    @Override
    public List<Patient> getPatientsByCreatedBy(UUID createdById) {
        return patientRepository.findByCreatedBy_Id(createdById);
    }

    @Override
    public List<Patient> getPatientsByGender(String gender) {
        return patientRepository.findByGender(gender); // gender nie jest szyfrowane
    }

    @Override
    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    @Override
    public void deletePatientById(UUID patientId) {
        patientRepository.deleteById(patientId);
    }

    @Override
    public Optional<Patient> findPatientById(UUID patientId) {
        return patientRepository.findById(patientId);
    }

    @Override
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Override
    public boolean existsById(UUID id) {
        return patientRepository.existsById(id);
    }

    @Override
    public Optional<Patient> updatePatient(UUID id, Patient update) {
        return patientRepository.findById(id).map(existing -> {
            existing.setFirstName(update.getFirstName());
            existing.setLastName(update.getLastName());
            existing.setPesel(update.getPesel());
            existing.setContactInfo(update.getContactInfo());
            existing.setAddress(update.getAddress());
            return patientRepository.save(existing);
        });
    }

    // Nowe metody DTO + autoryzacja
    @Override
    @Transactional(readOnly = true)
    public List<PatientDto> getAllVisibleForCurrent() {
        User u = current();
        return patientRepository.findAll().stream()
                .filter(p -> authorizationService.canReadPatient(u, p))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientDto> getVisibleById(UUID id) {
        User u = current();
        return patientRepository.findById(id)
                .filter(p -> authorizationService.canReadPatient(u, p))
                .map(this::toDto);
    }

    @Override
    @Transactional
    public PatientDto createForCurrent(Patient patient) {
        User u = current();
        if (u == null) throw new SecurityException("Brak autoryzacji");
        patient.setCreatedBy(u);
        if (!authorizationService.canWritePatient(u, patient)) {
            throw new SecurityException("Brak uprawnień do utworzenia pacjenta");
        }
        // saveAndFlush wymusza natychmiastowy INSERT do tabeli patients zanim
        // dopiszemy rekord do audit_log (który ma FK -> patients). Bez tego
        // Hibernate może zmienić kolejność INSERT-ów przy commicie i FK się wywali.
        Patient saved = patientRepository.saveAndFlush(patient);
        auditService.logPatient(u, saved, "CREATE_PATIENT", "Utworzono pacjenta id=" + saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public Optional<PatientDto> updateForCurrent(UUID id, Patient update) {
        User u = current();
        if (u == null) throw new SecurityException("Brak autoryzacji");
        return patientRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing)) {
                throw new SecurityException("Brak uprawnień do aktualizacji pacjenta");
            }
            Patient before = new Patient();
            before.setId(existing.getId());
            before.setFirstName(existing.getFirstName());
            before.setLastName(existing.getLastName());
            before.setPesel(existing.getPesel());
            before.setContactInfo(existing.getContactInfo());
            before.setAddress(existing.getAddress());
            before.setDateOfBirth(existing.getDateOfBirth());
            before.setGender(existing.getGender());

            existing.setFirstName(update.getFirstName());
            existing.setLastName(update.getLastName());
            existing.setPesel(update.getPesel());
            existing.setContactInfo(update.getContactInfo());
            existing.setAddress(update.getAddress());
            Patient saved = patientRepository.save(existing);
            String diff = auditService.diffPatient(before, saved);
            auditService.logPatient(u, saved, "UPDATE_PATIENT", diff.isBlank()?"Brak zmian":diff);
            return toDto(saved);
        });
    }

    @Override
    @Transactional
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        if (u == null) throw new SecurityException("Brak autoryzacji");
        return patientRepository.findById(id).map(existing -> {
            if (!authorizationService.canDeletePatient(u, existing)) {
                throw new SecurityException("Brak uprawnień do usunięcia pacjenta");
            }
            auditService.logPatient(u, existing, "DELETE_PATIENT", "Usunięto pacjenta id=" + existing.getId());
            patientRepository.delete(existing);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findPatientIdByPesel(String pesel) {
        if (pesel == null || pesel.isBlank()) return Optional.empty();
        String trimmed = pesel.trim();
        // Try plaintext first (data may not be encrypted)
        Optional<Patient> byPlain = patientRepository.findByPesel(trimmed);
        if (byPlain.isPresent()) return byPlain.map(Patient::getId);
        // Fallback: encrypted lookup
        String encPesel = encDet("patient.pesel", trimmed);
        return patientRepository.findByPesel(encPesel).map(Patient::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientDto> searchVisible(
            java.util.Optional<String> firstName,
            java.util.Optional<String> lastName,
            java.util.Optional<String> pesel,
            java.util.Optional<java.time.LocalDate> dobStart,
            java.util.Optional<java.time.LocalDate> dobEnd,
            java.util.Optional<String> gender,
            java.util.Optional<String> addressFragment,
            java.util.Optional<java.util.UUID> createdById,
            Pageable pageable
    ) {
        User u = current();
        // encrypt deterministic fields to match DB (encrypted), but PESEL may be plain
        var encFirst = firstName.isPresent() ? java.util.Optional.ofNullable(encDet("patient.first_name", firstName.get())) : java.util.Optional.<String>empty();
        var encLast = lastName.isPresent() ? java.util.Optional.ofNullable(encDet("patient.last_name", lastName.get())) : java.util.Optional.<String>empty();
        // PESEL search: try plaintext first (DB may store plain if migration hasn't run)
        var encPes = pesel; // pass plaintext to spec; spec does exact match against DB
        var spec = PatientSpecifications.withFilters(encFirst, encLast, encPes, dobStart, dobEnd, gender, addressFragment, createdById);
        var all = patientRepository.findAll(spec);
        var visible = all.stream()
                .filter(p -> authorizationService.canReadPatient(u, p))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }
}