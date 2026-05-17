package com.example.emr_server.service;

import com.example.emr_server.controller.dto.PatientDto;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PatientRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.spec.PatientSpecifications;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.security.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PatientServiceImpl}.
 * Covers: create/update/delete flows with authorization + audit, FK-safe saveAndFlush,
 * read filtering, PESEL lookup (plain & encrypted fallback), paged search.
 */
@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock private PatientRepository patientRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private AuthorizationService authorizationService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private PatientServiceImpl service;

    private User doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setUsername("doc1");
        doctor.setRole("doctor");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("Jan");
        patient.setLastName("Kowalski");
        patient.setPesel("12345678901");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender("M");
    }

    // ── createForCurrent ─────────────────────────────────────────────────

    @Test
    void createForCurrent_authorizedUser_savesAndFlushesPatientThenAudits() {
        // Given: authorized doctor, repository returns saved patient with id
        Patient input = new Patient();
        input.setFirstName("Anna");
        input.setLastName("Nowak");
        input.setPesel("99999999999");
        input.setDateOfBirth(LocalDate.of(1985, 5, 5));

        when(authorizationService.canWritePatient(eq(doctor), any(Patient.class))).thenReturn(true);
        when(patientRepository.saveAndFlush(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        // When
        PatientDto dto;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            dto = service.createForCurrent(input);
        }

        // Then: saveAndFlush used (FK ordering), createdBy set, audit logged
        assertThat(dto).isNotNull();
        assertThat(dto.firstName()).isEqualTo("Anna");
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(doctor);
        verify(auditService).logPatient(eq(doctor), any(Patient.class), eq("CREATE_PATIENT"), anyString());
        verify(patientRepository, never()).save(any());
    }

    @Test
    void createForCurrent_noCurrentUser_throwsSecurityException() {
        // Given
        Patient input = new Patient();

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createForCurrent(input))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("autoryzacji");
        }
        verify(patientRepository, never()).saveAndFlush(any());
        verify(auditService, never()).logPatient(any(), any(), anyString(), anyString());
    }

    @Test
    void createForCurrent_userWithoutWriteAccess_throwsAndDoesNotSave() {
        // Given
        Patient input = new Patient();
        when(authorizationService.canWritePatient(eq(doctor), any(Patient.class))).thenReturn(false);

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.createForCurrent(input))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Brak uprawnień");
        }
        verify(patientRepository, never()).saveAndFlush(any());
        verify(auditService, never()).logPatient(any(), any(), anyString(), anyString());
    }

    // ── updateForCurrent ─────────────────────────────────────────────────

    @Test
    void updateForCurrent_existingAndAuthorized_updatesAndAuditsDiff() {
        // Given: persisted patient, doctor allowed to write
        Patient update = new Patient();
        update.setFirstName("NewName");
        update.setLastName("NewLast");
        update.setPesel("00000000000");
        update.setAddress("ul. Inna 1");

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.diffPatient(any(), any())).thenReturn("firstName: 'Jan'->'NewName'");

        // When
        Optional<PatientDto> result;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            result = service.updateForCurrent(patient.getId(), update);
        }

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().firstName()).isEqualTo("NewName");
        verify(patientRepository).save(patient);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_PATIENT"), anyString());
    }

    @Test
    void updateForCurrent_blankDiff_logsBrakZmianMessage() {
        // Given
        Patient update = new Patient();
        update.setFirstName(patient.getFirstName());
        update.setLastName(patient.getLastName());
        update.setPesel(patient.getPesel());

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditService.diffPatient(any(), any())).thenReturn("");

        // When
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            service.updateForCurrent(patient.getId(), update);
        }

        // Then
        ArgumentCaptor<String> desc = ArgumentCaptor.forClass(String.class);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_PATIENT"), desc.capture());
        assertThat(desc.getValue()).isEqualTo("Brak zmian");
    }

    @Test
    void updateForCurrent_notFound_returnsEmptyAndNoAudit() {
        // Given
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        // When
        Optional<PatientDto> result;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            result = service.updateForCurrent(id, new Patient());
        }

        // Then
        assertThat(result).isEmpty();
        verify(patientRepository, never()).save(any());
        verify(auditService, never()).logPatient(any(), any(), anyString(), anyString());
    }

    @Test
    void updateForCurrent_noWriteAccess_throwsSecurityException() {
        // Given
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.updateForCurrent(patient.getId(), new Patient()))
                    .isInstanceOf(SecurityException.class);
        }
        verify(patientRepository, never()).save(any());
    }

    @Test
    void updateForCurrent_noCurrentUser_throwsSecurityException() {
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateForCurrent(patient.getId(), new Patient()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ── deleteForCurrent ─────────────────────────────────────────────────

    @Test
    void deleteForCurrent_existingAndAuthorized_deletesAndAudits() {
        // Given
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canDeletePatient(doctor, patient)).thenReturn(true);

        // When
        boolean ok;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            ok = service.deleteForCurrent(patient.getId());
        }

        // Then
        assertThat(ok).isTrue();
        verify(patientRepository).delete(patient);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("DELETE_PATIENT"), anyString());
    }

    @Test
    void deleteForCurrent_notFound_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        boolean ok;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            ok = service.deleteForCurrent(id);
        }

        assertThat(ok).isFalse();
        verify(patientRepository, never()).delete(any(Patient.class));
        verify(auditService, never()).logPatient(any(), any(), anyString(), anyString());
    }

    @Test
    void deleteForCurrent_noDeletePermission_throwsSecurityException() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canDeletePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.deleteForCurrent(patient.getId()))
                    .isInstanceOf(SecurityException.class);
        }
        verify(patientRepository, never()).delete(any(Patient.class));
    }

    @Test
    void deleteForCurrent_noCurrentUser_throwsSecurityException() {
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteForCurrent(patient.getId()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ── getVisibleById / getAllVisibleForCurrent ─────────────────────────

    @Test
    void getVisibleById_authorizedRead_returnsDto() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<PatientDto> result;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            result = service.getVisibleById(patient.getId());
        }

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(patient.getId());
    }

    @Test
    void getVisibleById_noReadAccess_returnsEmpty() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(false);

        Optional<PatientDto> result;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            result = service.getVisibleById(patient.getId());
        }

        assertThat(result).isEmpty();
    }

    @Test
    void getAllVisibleForCurrent_filtersOutInaccessiblePatients() {
        // Given two patients: one accessible, one not
        Patient other = new Patient();
        other.setId(UUID.randomUUID());
        other.setFirstName("Inny");
        other.setLastName("Pacjent");
        other.setPesel("11111111111");
        other.setDateOfBirth(LocalDate.of(1970, 1, 1));

        when(patientRepository.findAll()).thenReturn(List.of(patient, other));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, other)).thenReturn(false);

        // When
        List<PatientDto> result;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            result = service.getAllVisibleForCurrent();
        }

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(patient.getId());
    }

    // ── findPatientIdByPesel ─────────────────────────────────────────────

    @Test
    void findPatientIdByPesel_plaintextHit_returnsIdWithoutEncryption() {
        when(patientRepository.findByPesel("12345678901")).thenReturn(Optional.of(patient));

        Optional<UUID> result = service.findPatientIdByPesel("12345678901");

        assertThat(result).contains(patient.getId());
        verify(encryptionService, never()).encryptDeterministic(anyString(), anyString());
    }

    @Test
    void findPatientIdByPesel_plaintextMiss_fallsBackToEncrypted() {
        // Given
        when(patientRepository.findByPesel("12345678901")).thenReturn(Optional.empty());
        when(encryptionService.encryptDeterministic("patient.pesel", "12345678901")).thenReturn("enc_pesel");
        when(patientRepository.findByPesel("enc_pesel")).thenReturn(Optional.of(patient));

        // When
        Optional<UUID> result = service.findPatientIdByPesel("12345678901");

        // Then
        assertThat(result).contains(patient.getId());
        verify(encryptionService).encryptDeterministic("patient.pesel", "12345678901");
    }

    @Test
    void findPatientIdByPesel_blankInput_returnsEmpty() {
        assertThat(service.findPatientIdByPesel("")).isEmpty();
        assertThat(service.findPatientIdByPesel(null)).isEmpty();
        verify(patientRepository, never()).findByPesel(anyString());
    }

    // ── getPatientByPesel (uses deterministic encryption) ────────────────

    @Test
    void getPatientByPesel_encryptsBeforeLookup() {
        when(encryptionService.encryptDeterministic("patient.pesel", "12345678901")).thenReturn("enc");
        when(patientRepository.findByPesel("enc")).thenReturn(Optional.of(patient));

        Optional<Patient> result = service.getPatientByPesel("12345678901");

        assertThat(result).contains(patient);
    }

    // ── searchVisible ────────────────────────────────────────────────────

    @Test
    void searchVisible_filtersByAuthorizationAndPaginates() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Patient hidden = new Patient();
        hidden.setId(UUID.randomUUID());
        hidden.setFirstName("Hidden");
        hidden.setLastName("X");
        hidden.setPesel("22222222222");
        hidden.setDateOfBirth(LocalDate.of(1980, 1, 1));

        when(encryptionService.encryptDeterministic("patient.first_name", "Jan")).thenReturn("enc_jan");
        when(patientRepository.findAll(Mockito.<Specification<Patient>>any()))
                .thenReturn(List.of(patient, hidden));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, hidden)).thenReturn(false);

        // When
        Page<PatientDto> page;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            page = service.searchVisible(
                    Optional.of("Jan"), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), pageable);
        }

        // Then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(patient.getId());
    }

    // ── simple delegation ────────────────────────────────────────────────

    @Test
    void savePatient_delegatesToRepository() {
        when(patientRepository.save(patient)).thenReturn(patient);
        assertThat(service.savePatient(patient)).isEqualTo(patient);
        verify(patientRepository).save(patient);
    }

    @Test
    void deletePatientById_delegatesToRepository() {
        service.deletePatientById(patient.getId());
        verify(patientRepository).deleteById(patient.getId());
    }

    @Test
    void existsById_delegatesToRepository() {
        when(patientRepository.existsById(patient.getId())).thenReturn(true);
        assertThat(service.existsById(patient.getId())).isTrue();
    }

    @Test
    void findAll_delegatesToRepository() {
        when(patientRepository.findAll()).thenReturn(List.of(patient));
        assertThat(service.findAll()).containsExactly(patient);
    }
}

