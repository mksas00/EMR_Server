package com.example.emr_server.service;

import com.example.emr_server.controller.dto.LabResultDto;
import com.example.emr_server.entity.LabResult;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.LabResultRepository;
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
 * Unit tests for {@link LabResultServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class LabResultServiceImplTest {

    @Mock private LabResultRepository labResultRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private PatientService patientService;

    @InjectMocks
    private LabResultServiceImpl service;

    private User doctor;
    private Patient patient;
    private LabResult result;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setUsername("doc");
        doctor.setRole("doctor");

        patient = new Patient();
        patient.setId(UUID.randomUUID());

        result = new LabResult();
        result.setId(UUID.randomUUID());
        result.setPatient(patient);
        result.setTestName("CRP");
        result.setResult("5");
        result.setUnit("mg/L");
        result.setStatus("FINAL");
        result.setResultDate(LocalDate.of(2026, 5, 1));
    }

    private MockedStatic<SecurityUtil> mockCurrent(User u) {
        MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class);
        m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.ofNullable(u));
        return m;
    }

    @Test
    void createForCurrent_authorized_savesAndAudits() {
        LabResult input = new LabResult();
        input.setPatient(patient);
        input.setTestName("Glucose");
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(labResultRepository.save(input)).thenAnswer(inv -> {
            LabResult r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        LabResultDto dto;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            dto = service.createForCurrent(input);
        }

        assertThat(dto.testName()).isEqualTo("Glucose");
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_LAB_RESULT"), anyString());
    }

    @Test
    void createForCurrent_unauthorized_throws() {
        LabResult input = new LabResult();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.createForCurrent(input))
                    .isInstanceOf(SecurityException.class);
        }
        verify(labResultRepository, never()).save(any());
    }

    @Test
    void updateForCurrent_authorized_updatesAllFieldsAndAudits() {
        LabResult update = new LabResult();
        update.setTestName("Hemoglobin");
        update.setResult("14");
        update.setUnit("g/dL");
        update.setReferenceRange("12-16");
        update.setStatus("PRELIMINARY");
        update.setResultDate(LocalDate.of(2026, 5, 10));
        when(labResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(labResultRepository.save(result)).thenReturn(result);

        Optional<LabResultDto> res;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            res = service.updateForCurrent(result.getId(), update);
        }

        assertThat(res).isPresent();
        assertThat(result.getTestName()).isEqualTo("Hemoglobin");
        assertThat(result.getResult()).isEqualTo("14");
        assertThat(result.getStatus()).isEqualTo("PRELIMINARY");
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_LAB_RESULT"), anyString());
    }

    @Test
    void updateForCurrent_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(labResultRepository.findById(id)).thenReturn(Optional.empty());

        Optional<LabResultDto> res;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            res = service.updateForCurrent(id, new LabResult());
        }

        assertThat(res).isEmpty();
    }

    @Test
    void updateForCurrent_unauthorized_throws() {
        when(labResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.updateForCurrent(result.getId(), new LabResult()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Test
    void deleteForCurrent_existingAuthorized_deletesAndAudits() {
        when(labResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(result.getId());
        }

        assertThat(ok).isTrue();
        verify(labResultRepository).delete(result);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("DELETE_LAB_RESULT"), anyString());
    }

    @Test
    void deleteForCurrent_notFound_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(labResultRepository.findById(id)).thenReturn(Optional.empty());

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(id);
        }

        assertThat(ok).isFalse();
        verify(labResultRepository, never()).delete(any(LabResult.class));
    }

    @Test
    void deleteForCurrent_unauthorized_throws() {
        when(labResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.deleteForCurrent(result.getId()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Test
    void saveLabResult_newEntity_logsCreate() {
        LabResult input = new LabResult();
        input.setPatient(patient);
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(labResultRepository.save(input)).thenAnswer(inv -> {
            LabResult r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.saveLabResult(input);
        }

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_LAB_RESULT"), anyString());
    }

    @Test
    void saveLabResult_existingEntity_logsUpdate() {
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(labResultRepository.save(result)).thenReturn(result);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.saveLabResult(result);
        }

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_LAB_RESULT"), anyString());
    }

    @Test
    void getVisibleById_authorized_returnsDto() {
        when(labResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<LabResultDto> dto;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            dto = service.getVisibleById(result.getId());
        }

        assertThat(dto).isPresent();
    }

    @Test
    void getVisibleById_notAuthorized_returnsEmpty() {
        when(labResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(false);

        Optional<LabResultDto> dto;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            dto = service.getVisibleById(result.getId());
        }

        assertThat(dto).isEmpty();
    }

    @Test
    void searchVisible_withPesel_resolves() {
        when(patientService.findPatientIdByPesel("12345678901")).thenReturn(Optional.of(patient.getId()));
        when(labResultRepository.findAll(Mockito.<Specification<LabResult>>any())).thenReturn(List.of(result));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Page<LabResultDto> page;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            page = service.searchVisible(Optional.empty(), Optional.of("12345678901"),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    PageRequest.of(0, 10));
        }

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void searchVisible_peselNotFound_returnsEmpty() {
        when(patientService.findPatientIdByPesel("00000000000")).thenReturn(Optional.empty());

        Page<LabResultDto> page = service.searchVisible(Optional.empty(), Optional.of("00000000000"),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }
}

