package com.example.emr_server.service;

import com.example.emr_server.controller.dto.ChronicDiseaseDto;
import com.example.emr_server.entity.ChronicDisease;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.ChronicDiseaseRepository;
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
 * Unit tests for {@link ChronicDiseaseServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ChronicDiseaseServiceImplTest {

    @Mock private ChronicDiseaseRepository chronicDiseaseRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private ChronicDiseaseServiceImpl service;

    private User doctor;
    private Patient patient;
    private ChronicDisease disease;

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

        disease = new ChronicDisease();
        disease.setId(UUID.randomUUID());
        disease.setPatient(patient);
        disease.setDiseaseName("Cukrzyca");
        disease.setDiagnosedDate(LocalDate.of(2020, 1, 1));
        disease.setNotes("Typ II");
    }

    private MockedStatic<SecurityUtil> mockCurrent(User u) {
        MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class);
        m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.ofNullable(u));
        return m;
    }

    @Test
    void createForCurrent_authorized_savesAndAudits() {
        ChronicDisease input = new ChronicDisease();
        input.setPatient(patient);
        input.setDiseaseName("Nadciśnienie");
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(chronicDiseaseRepository.save(input)).thenAnswer(inv -> {
            ChronicDisease cd = inv.getArgument(0);
            cd.setId(UUID.randomUUID());
            return cd;
        });

        ChronicDiseaseDto dto;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            dto = service.createForCurrent(input);
        }

        assertThat(dto.diseaseName()).isEqualTo("Nadciśnienie");
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_CHRONIC_DISEASE"), anyString());
    }

    @Test
    void createForCurrent_unauthorized_throws() {
        ChronicDisease input = new ChronicDisease();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.createForCurrent(input))
                    .isInstanceOf(SecurityException.class);
        }
        verify(chronicDiseaseRepository, never()).save(any());
    }

    @Test
    void updateForCurrent_existing_updatesFieldsAndAudits() {
        ChronicDisease update = new ChronicDisease();
        update.setDiseaseName("Astma");
        update.setDiagnosedDate(LocalDate.of(2022, 6, 1));
        update.setNotes("kontrola roczna");
        when(chronicDiseaseRepository.findById(disease.getId())).thenReturn(Optional.of(disease));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(chronicDiseaseRepository.save(disease)).thenReturn(disease);

        Optional<ChronicDiseaseDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.updateForCurrent(disease.getId(), update);
        }

        assertThat(result).isPresent();
        assertThat(disease.getDiseaseName()).isEqualTo("Astma");
        assertThat(disease.getDiagnosedDate()).isEqualTo(LocalDate.of(2022, 6, 1));
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_CHRONIC_DISEASE"), anyString());
    }

    @Test
    void updateForCurrent_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(chronicDiseaseRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ChronicDiseaseDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.updateForCurrent(id, new ChronicDisease());
        }

        assertThat(result).isEmpty();
        verify(chronicDiseaseRepository, never()).save(any());
    }

    @Test
    void updateForCurrent_unauthorized_throws() {
        when(chronicDiseaseRepository.findById(disease.getId())).thenReturn(Optional.of(disease));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.updateForCurrent(disease.getId(), new ChronicDisease()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Test
    void deleteForCurrent_existing_deletesAndAudits() {
        when(chronicDiseaseRepository.findById(disease.getId())).thenReturn(Optional.of(disease));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(disease.getId());
        }

        assertThat(ok).isTrue();
        verify(chronicDiseaseRepository).delete(disease);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("DELETE_CHRONIC_DISEASE"), anyString());
    }

    @Test
    void deleteForCurrent_notFound_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(chronicDiseaseRepository.findById(id)).thenReturn(Optional.empty());

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(id);
        }

        assertThat(ok).isFalse();
        verify(chronicDiseaseRepository, never()).delete(any(ChronicDisease.class));
    }

    @Test
    void deleteForCurrent_unauthorized_throws() {
        when(chronicDiseaseRepository.findById(disease.getId())).thenReturn(Optional.of(disease));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.deleteForCurrent(disease.getId()))
                    .isInstanceOf(SecurityException.class);
        }
        verify(chronicDiseaseRepository, never()).delete(any(ChronicDisease.class));
    }

    @Test
    void saveDisease_newEntity_logsCreate() {
        ChronicDisease input = new ChronicDisease();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(chronicDiseaseRepository.save(input)).thenAnswer(inv -> {
            ChronicDisease cd = inv.getArgument(0);
            cd.setId(UUID.randomUUID());
            return cd;
        });

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.saveDisease(input);
        }

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_CHRONIC_DISEASE"), anyString());
    }

    @Test
    void saveDisease_existingEntity_logsUpdate() {
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(chronicDiseaseRepository.save(disease)).thenReturn(disease);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.saveDisease(disease);
        }

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_CHRONIC_DISEASE"), anyString());
    }

    @Test
    void getVisibleById_authorized_returnsDto() {
        when(chronicDiseaseRepository.findById(disease.getId())).thenReturn(Optional.of(disease));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<ChronicDiseaseDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getVisibleById(disease.getId());
        }

        assertThat(result).isPresent();
    }

    @Test
    void getVisibleById_notAuthorized_returnsEmpty() {
        when(chronicDiseaseRepository.findById(disease.getId())).thenReturn(Optional.of(disease));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(false);

        Optional<ChronicDiseaseDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getVisibleById(disease.getId());
        }

        assertThat(result).isEmpty();
    }

    @Test
    void searchVisible_filtersInaccessibleAndPaginates() {
        ChronicDisease hidden = new ChronicDisease();
        Patient p2 = new Patient();
        p2.setId(UUID.randomUUID());
        hidden.setPatient(p2);

        when(chronicDiseaseRepository.findAll(Mockito.<Specification<ChronicDisease>>any()))
                .thenReturn(List.of(disease, hidden));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, p2)).thenReturn(false);

        Page<ChronicDiseaseDto> page;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            page = service.searchVisible(Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), PageRequest.of(0, 10));
        }

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getDiseasesByPatientId_filtersInaccessible() {
        ChronicDisease other = new ChronicDisease();
        Patient p2 = new Patient();
        p2.setId(UUID.randomUUID());
        other.setPatient(p2);
        when(chronicDiseaseRepository.findByPatient_Id(patient.getId())).thenReturn(List.of(disease, other));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, p2)).thenReturn(false);

        List<ChronicDisease> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getDiseasesByPatientId(patient.getId());
        }

        assertThat(result).containsExactly(disease);
    }
}

