package com.example.emr_server.service;

import com.example.emr_server.controller.dto.AllergyDto;
import com.example.emr_server.entity.Allergy;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.AllergyRepository;
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
 * Unit tests for {@link AllergyServiceImpl}.
 * Covers create/update/delete authorization, audit logging, read filtering and pageable search.
 */
@ExtendWith(MockitoExtension.class)
class AllergyServiceImplTest {

    @Mock private AllergyRepository allergyRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private AllergyServiceImpl service;

    private User doctor;
    private Patient patient;
    private Allergy allergy;

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

        allergy = new Allergy();
        allergy.setId(UUID.randomUUID());
        allergy.setPatient(patient);
        allergy.setAllergen("Pyłki");
        allergy.setReaction("Katar");
        allergy.setSeverity("LOW");
    }

    private MockedStatic<SecurityUtil> mockCurrent(User u) {
        MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class);
        m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.ofNullable(u));
        return m;
    }

    // ── createForCurrent ─────────────────────────────────────────────────

    @Test
    void createForCurrent_authorized_savesAndAudits() {
        // Given
        Allergy input = new Allergy();
        input.setPatient(patient);
        input.setAllergen("Orzechy");
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(allergyRepository.save(input)).thenAnswer(inv -> {
            Allergy a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        // When
        AllergyDto dto;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            dto = service.createForCurrent(input);
        }

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.allergen()).isEqualTo("Orzechy");
        verify(allergyRepository).save(input);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_ALLERGY"), anyString());
    }

    @Test
    void createForCurrent_noWriteAccess_throwsAndDoesNotSave() {
        Allergy input = new Allergy();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.createForCurrent(input))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Brak uprawnień");
        }
        verify(allergyRepository, never()).save(any());
        verify(auditService, never()).logPatient(any(), any(), anyString(), anyString());
    }

    // ── updateForCurrent ─────────────────────────────────────────────────

    @Test
    void updateForCurrent_existingAndAuthorized_updatesFields() {
        // Given
        Allergy update = new Allergy();
        update.setAllergen("NewAllergen");
        update.setReaction("NewReaction");
        update.setSeverity("HIGH");
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(allergyRepository.save(allergy)).thenReturn(allergy);

        // When
        Optional<AllergyDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.updateForCurrent(allergy.getId(), update);
        }

        // Then
        assertThat(result).isPresent();
        assertThat(allergy.getAllergen()).isEqualTo("NewAllergen");
        assertThat(allergy.getReaction()).isEqualTo("NewReaction");
        assertThat(allergy.getSeverity()).isEqualTo("HIGH");
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_ALLERGY"), anyString());
    }

    @Test
    void updateForCurrent_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(allergyRepository.findById(id)).thenReturn(Optional.empty());

        Optional<AllergyDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.updateForCurrent(id, new Allergy());
        }

        assertThat(result).isEmpty();
        verify(allergyRepository, never()).save(any());
    }

    @Test
    void updateForCurrent_noWriteAccess_throws() {
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.updateForCurrent(allergy.getId(), new Allergy()))
                    .isInstanceOf(SecurityException.class);
        }
        verify(allergyRepository, never()).save(any());
    }

    // ── deleteForCurrent ─────────────────────────────────────────────────

    @Test
    void deleteForCurrent_existingAndAuthorized_deletesAndAudits() {
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(allergy.getId());
        }

        assertThat(ok).isTrue();
        verify(allergyRepository).delete(allergy);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("DELETE_ALLERGY"), anyString());
    }

    @Test
    void deleteForCurrent_notFound_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(allergyRepository.findById(id)).thenReturn(Optional.empty());

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(id);
        }

        assertThat(ok).isFalse();
        verify(allergyRepository, never()).delete(any(Allergy.class));
    }

    @Test
    void deleteForCurrent_noWriteAccess_throws() {
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.deleteForCurrent(allergy.getId()))
                    .isInstanceOf(SecurityException.class);
        }
        verify(allergyRepository, never()).delete(any(Allergy.class));
    }

    // ── saveAllergy (create vs update branch) ────────────────────────────

    @Test
    void saveAllergy_newEntity_logsCreateAction() {
        Allergy input = new Allergy();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(allergyRepository.save(input)).thenAnswer(inv -> {
            Allergy a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.saveAllergy(input);
        }

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_ALLERGY"), anyString());
    }

    @Test
    void saveAllergy_existingEntity_logsUpdateAction() {
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(allergyRepository.save(allergy)).thenReturn(allergy);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.saveAllergy(allergy);
        }

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_ALLERGY"), anyString());
    }

    // ── deleteAllergy ────────────────────────────────────────────────────

    @Test
    void deleteAllergy_notFound_doesNothing() {
        UUID id = UUID.randomUUID();
        when(allergyRepository.findById(id)).thenReturn(Optional.empty());

        service.deleteAllergy(id);

        verify(allergyRepository, never()).delete(any(Allergy.class));
        verify(auditService, never()).logPatient(any(), any(), anyString(), anyString());
    }

    @Test
    void deleteAllergy_noWriteAccess_throws() {
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.deleteAllergy(allergy.getId()))
                    .isInstanceOf(SecurityException.class);
        }
        verify(allergyRepository, never()).delete(any(Allergy.class));
    }

    // ── read paths ───────────────────────────────────────────────────────

    @Test
    void getAllergiesByPatientId_filtersInaccessible() {
        Allergy a2 = new Allergy();
        Patient p2 = new Patient();
        p2.setId(UUID.randomUUID());
        a2.setPatient(p2);
        when(allergyRepository.findByPatient_Id(patient.getId())).thenReturn(List.of(allergy, a2));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, p2)).thenReturn(false);

        List<Allergy> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getAllergiesByPatientId(patient.getId());
        }

        assertThat(result).containsExactly(allergy);
    }

    @Test
    void getVisibleById_authorized_returnsDto() {
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<AllergyDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getVisibleById(allergy.getId());
        }

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(allergy.getId());
    }

    @Test
    void getVisibleById_notAuthorized_returnsEmpty() {
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(false);

        Optional<AllergyDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getVisibleById(allergy.getId());
        }

        assertThat(result).isEmpty();
    }

    @Test
    void searchVisible_filtersOutNonReadableAndPaginates() {
        Allergy hidden = new Allergy();
        Patient p2 = new Patient();
        p2.setId(UUID.randomUUID());
        hidden.setPatient(p2);

        when(allergyRepository.findAll(Mockito.<Specification<Allergy>>any()))
                .thenReturn(List.of(allergy, hidden));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, p2)).thenReturn(false);

        Page<AllergyDto> page;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            page = service.searchVisible(Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), PageRequest.of(0, 10));
        }

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(allergy.getId());
    }
}

