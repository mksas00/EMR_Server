package com.example.emr_server.service;

import com.example.emr_server.controller.dto.PrescriptionDto;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.Prescription;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PrescriptionRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
 * Unit tests for {@link PrescriptionServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private PatientService patientService;

    @InjectMocks
    private PrescriptionServiceImpl service;

    private User doctor;
    private Patient patient;
    private Prescription prescription;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setUsername("doc");
        doctor.setRole("doctor");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("Jan");
        patient.setLastName("Kowalski");

        prescription = new Prescription();
        prescription.setId(UUID.randomUUID());
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setIssuedDate(LocalDate.of(2026, 5, 14));
        prescription.setExpirationDate(LocalDate.of(2026, 6, 13));
    }

    private MockedStatic<SecurityUtil> mockCurrent(User u) {
        MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class);
        m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.ofNullable(u));
        return m;
    }

    // ── createForCurrent ─────────────────────────────────────────────────

    @Test
    void createForCurrent_authorized_savesWithDefaultsAndAudits() {
        // Given: input without issuedDate/expirationDate
        Prescription input = new Prescription();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(prescriptionRepository.save(input)).thenAnswer(inv -> {
            Prescription p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        // When
        PrescriptionDto dto;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            dto = service.createForCurrent(input);
        }

        // Then: doctor set, issuedDate defaults to today, expirationDate = issued + 30d
        assertThat(dto).isNotNull();
        assertThat(input.getDoctor()).isEqualTo(doctor);
        assertThat(input.getIssuedDate()).isEqualTo(LocalDate.now());
        assertThat(input.getExpirationDate()).isEqualTo(input.getIssuedDate().plusDays(30));
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_PRESCRIPTION"), anyString());
    }

    @Test
    void createForCurrent_explicitDates_doesNotOverride() {
        Prescription input = new Prescription();
        input.setPatient(patient);
        input.setIssuedDate(LocalDate.of(2026, 1, 1));
        input.setExpirationDate(LocalDate.of(2026, 1, 31));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(prescriptionRepository.save(input)).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.createForCurrent(input);
        }

        assertThat(input.getIssuedDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(input.getExpirationDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void createForCurrent_unauthorized_throws() {
        Prescription input = new Prescription();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.createForCurrent(input))
                    .isInstanceOf(SecurityException.class);
        }
        verify(prescriptionRepository, never()).save(any());
    }

    // ── updateForCurrent ─────────────────────────────────────────────────

    @Test
    void updateForCurrent_authorized_updatesAndAudits() {
        Prescription update = new Prescription();
        update.setDosageInfo("rano 1 tabletka");
        update.setIssuedDate(LocalDate.of(2026, 5, 1));
        update.setExpirationDate(LocalDate.of(2026, 5, 31));
        update.setIsRepeatable(true);
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(prescriptionRepository.save(prescription)).thenReturn(prescription);

        Optional<PrescriptionDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.updateForCurrent(prescription.getId(), update);
        }

        assertThat(result).isPresent();
        assertThat(prescription.getDosageInfo()).isEqualTo("rano 1 tabletka");
        assertThat(prescription.getIsRepeatable()).isTrue();
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_PRESCRIPTION"), anyString());
    }

    @Test
    void updateForCurrent_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(prescriptionRepository.findById(id)).thenReturn(Optional.empty());

        Optional<PrescriptionDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.updateForCurrent(id, new Prescription());
        }

        assertThat(result).isEmpty();
    }

    @Test
    void updateForCurrent_unauthorized_throws() {
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.updateForCurrent(prescription.getId(), new Prescription()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ── deleteForCurrent ─────────────────────────────────────────────────

    @Test
    void deleteForCurrent_existing_deletesAndAudits() {
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(prescription.getId());
        }

        assertThat(ok).isTrue();
        verify(prescriptionRepository).delete(prescription);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("DELETE_PRESCRIPTION"), anyString());
    }

    @Test
    void deleteForCurrent_notFound_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(prescriptionRepository.findById(id)).thenReturn(Optional.empty());

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(id);
        }

        assertThat(ok).isFalse();
        verify(prescriptionRepository, never()).delete(any(Prescription.class));
    }

    // ── getPrescriptionsByDoctorId – own vs other doctor branch ─────────

    @Test
    void getPrescriptionsByDoctorId_ownPrescriptions_returnsAllRegardlessOfReadAccess() {
        when(prescriptionRepository.findByDoctor_Id(doctor.getId())).thenReturn(List.of(prescription));

        List<Prescription> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getPrescriptionsByDoctorId(doctor.getId());
        }

        assertThat(result).containsExactly(prescription);
        verify(authorizationService, never()).canReadPatient(any(), any());
    }

    @Test
    void getPrescriptionsByDoctorId_otherDoctorPrescriptions_appliesReadFilter() {
        UUID otherDoctorId = UUID.randomUUID();
        when(prescriptionRepository.findByDoctor_Id(otherDoctorId)).thenReturn(List.of(prescription));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        List<Prescription> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getPrescriptionsByDoctorId(otherDoctorId);
        }

        assertThat(result).containsExactly(prescription);
        verify(authorizationService).canReadPatient(doctor, patient);
    }

    @Test
    void getPrescriptionsByDoctorId_noCurrentUser_returnsEmpty() {
        UUID otherDoctorId = UUID.randomUUID();
        when(prescriptionRepository.findByDoctor_Id(otherDoctorId)).thenReturn(List.of(prescription));

        List<Prescription> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(null)) {
            result = service.getPrescriptionsByDoctorId(otherDoctorId);
        }

        assertThat(result).isEmpty();
    }

    // ── searchVisible ────────────────────────────────────────────────────

    @Test
    void searchVisible_withPesel_resolvesAndQueries() {
        when(patientService.findPatientIdByPesel("11111111111")).thenReturn(Optional.of(patient.getId()));
        when(prescriptionRepository.findAll(Mockito.<Specification<Prescription>>any())).thenReturn(List.of(prescription));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Page<PrescriptionDto> page;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            page = service.searchVisible(Optional.empty(), Optional.of("11111111111"),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), PageRequest.of(0, 10));
        }

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void searchVisible_peselNotFound_returnsEmpty() {
        when(patientService.findPatientIdByPesel("00000000000")).thenReturn(Optional.empty());

        Page<PrescriptionDto> page = service.searchVisible(Optional.empty(), Optional.of("00000000000"),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        verify(prescriptionRepository, never()).findAll(Mockito.<Specification<Prescription>>any());
    }

    @Test
    void searchVisible_filtersInaccessible() {
        Prescription hidden = new Prescription();
        Patient p2 = new Patient();
        p2.setId(UUID.randomUUID());
        hidden.setPatient(p2);
        when(prescriptionRepository.findAll(Mockito.<Specification<Prescription>>any()))
                .thenReturn(List.of(prescription, hidden));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, p2)).thenReturn(false);

        Page<PrescriptionDto> page;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            page = service.searchVisible(Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), PageRequest.of(0, 10));
        }

        assertThat(page.getContent()).hasSize(1);
    }
}

