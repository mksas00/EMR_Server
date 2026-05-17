package com.example.emr_server.service;

import com.example.emr_server.controller.dto.VisitDto;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.entity.Visit;
import com.example.emr_server.repository.PatientRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.VisitRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Unit tests for {@link VisitServiceImpl}.
 * Covers: create / update / delete with authorization + audit + collision detection,
 *         search via PESEL resolution, ICS export (single + calendar).
 */
@ExtendWith(MockitoExtension.class)
class VisitServiceImplTest {

    @Mock private VisitRepository visitRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private PatientRepository patientRepository;
    @Mock private PatientService patientService;

    @InjectMocks
    private VisitServiceImpl service;

    private User doctor;
    private Patient patient;
    private Visit visit;
    private Instant start;

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

        start = Instant.parse("2026-05-20T10:00:00Z");

        visit = new Visit();
        visit.setId(UUID.randomUUID());
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setVisitDate(start);
        visit.setEndDate(start.plus(30, ChronoUnit.MINUTES));
        visit.setVisitType("Konsultacja");
        visit.setReason("Powód");
        visit.setStatus(Visit.Status.PLANNED);
    }

    private MockedStatic<SecurityUtil> mockCurrent(User u) {
        MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class);
        m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.ofNullable(u));
        return m;
    }

    // ── createForCurrent ─────────────────────────────────────────────────

    @Test
    void createForCurrent_validNoCollision_savesAndAudits() {
        // Given
        Visit input = new Visit();
        input.setVisitDate(start);
        input.setEndDate(start.plus(30, ChronoUnit.MINUTES));
        input.setVisitType("Wizyta");

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(visitRepository.findOverlapping(eq(doctor.getId()), any(), any(), eq(Visit.Status.CANCELED)))
                .thenReturn(List.of());
        when(visitRepository.save(input)).thenAnswer(inv -> {
            Visit v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        // When
        VisitDto dto;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            dto = service.createForCurrent(patient.getId(), input);
        }

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.patientId()).isEqualTo(patient.getId());
        assertThat(input.getDoctor()).isEqualTo(doctor); // defaulted
        assertThat(input.getStatus()).isEqualTo(Visit.Status.PLANNED); // defaulted
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("CREATE_VISIT"), anyString());
    }

    @Test
    void createForCurrent_patientNotFound_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.createForCurrent(id, new Visit()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Pacjent nie istnieje");
        }
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createForCurrent_noWriteAccess_throwsSecurityException() {
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.createForCurrent(patient.getId(), new Visit()))
                    .isInstanceOf(SecurityException.class);
        }
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createForCurrent_collisionWithExistingVisit_throwsIllegalState() {
        // Given: another visit overlaps
        Visit existing = new Visit();
        existing.setId(UUID.randomUUID());

        Visit input = new Visit();
        input.setVisitDate(start);
        input.setEndDate(start.plus(30, ChronoUnit.MINUTES));

        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(visitRepository.findOverlapping(eq(doctor.getId()), any(), any(), eq(Visit.Status.CANCELED)))
                .thenReturn(List.of(existing));

        // When / Then
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.createForCurrent(patient.getId(), input))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Kolizja");
        }
        verify(visitRepository, never()).save(any());
    }

    @Test
    void createForCurrent_nullEndDate_defaultsTo30Minutes() {
        // Given
        Visit input = new Visit();
        input.setVisitDate(start);
        input.setEndDate(null);
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(visitRepository.findOverlapping(eq(doctor.getId()), eq(start), eq(start.plus(30, ChronoUnit.MINUTES)),
                eq(Visit.Status.CANCELED))).thenReturn(List.of());
        when(visitRepository.save(input)).thenAnswer(inv -> inv.getArgument(0));

        // When
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            service.createForCurrent(patient.getId(), input);
        }

        // Then: verify findOverlapping was called with start+30min as end
        verify(visitRepository).findOverlapping(eq(doctor.getId()), eq(start),
                eq(start.plus(30, ChronoUnit.MINUTES)), eq(Visit.Status.CANCELED));
    }

    // ── updateForCurrent ─────────────────────────────────────────────────

    @Test
    void updateForCurrent_existingAndAuthorized_updatesAndAudits() {
        Visit update = new Visit();
        update.setVisitDate(start);
        update.setEndDate(start.plus(45, ChronoUnit.MINUTES));
        update.setVisitType("Nowy typ");
        update.setStatus(Visit.Status.CONFIRMED);

        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(visitRepository.findOverlapping(eq(doctor.getId()), any(), any(), eq(Visit.Status.CANCELED)))
                .thenReturn(List.of(visit)); // tylko ten sam visit -> nie kolizja
        when(visitRepository.save(visit)).thenReturn(visit);

        Optional<VisitDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.updateForCurrent(visit.getId(), update);
        }

        assertThat(result).isPresent();
        assertThat(visit.getVisitType()).isEqualTo("Nowy typ");
        assertThat(visit.getStatus()).isEqualTo(Visit.Status.CONFIRMED);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("UPDATE_VISIT"), anyString());
    }

    @Test
    void updateForCurrent_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(visitRepository.findById(id)).thenReturn(Optional.empty());

        Optional<VisitDto> result = service.updateForCurrent(id, new Visit());

        assertThat(result).isEmpty();
        verify(visitRepository, never()).save(any());
    }

    @Test
    void updateForCurrent_unauthorized_throws() {
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.updateForCurrent(visit.getId(), new Visit()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ── deleteForCurrent ─────────────────────────────────────────────────

    @Test
    void deleteForCurrent_existingAndAuthorized_deletesAndAudits() {
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(visit.getId());
        }

        assertThat(ok).isTrue();
        verify(visitRepository).delete(visit);
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("DELETE_VISIT"), anyString());
    }

    @Test
    void deleteForCurrent_notFound_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(visitRepository.findById(id)).thenReturn(Optional.empty());

        boolean ok;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ok = service.deleteForCurrent(id);
        }

        assertThat(ok).isFalse();
        verify(visitRepository, never()).delete(any(Visit.class));
    }

    @Test
    void deleteForCurrent_unauthorized_throws() {
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            assertThatThrownBy(() -> service.deleteForCurrent(visit.getId()))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ── getVisibleById / read paths ─────────────────────────────────────

    @Test
    void getVisibleById_authorized_returnsDto() {
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<VisitDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getVisibleById(visit.getId());
        }

        assertThat(result).isPresent();
        assertThat(result.get().patientFullName()).isEqualTo("Jan Kowalski");
    }

    @Test
    void getVisibleById_notAuthorized_returnsEmpty() {
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(false);

        Optional<VisitDto> result;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            result = service.getVisibleById(visit.getId());
        }

        assertThat(result).isEmpty();
    }

    // ── searchVisible ────────────────────────────────────────────────────

    @Test
    void searchVisible_withPesel_resolvesPatientId() {
        when(patientService.findPatientIdByPesel("12345678901")).thenReturn(Optional.of(patient.getId()));
        when(visitRepository.findAll(Mockito.<Specification<Visit>>any())).thenReturn(List.of(visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Page<VisitDto> page;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            page = service.searchVisible(Optional.empty(), Optional.of("12345678901"),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), PageRequest.of(0, 10));
        }

        assertThat(page.getContent()).hasSize(1);
        verify(patientService).findPatientIdByPesel("12345678901");
    }

    @Test
    void searchVisible_peselNotFound_returnsEmptyPageWithoutQuery() {
        when(patientService.findPatientIdByPesel("00000000000")).thenReturn(Optional.empty());

        Page<VisitDto> page = service.searchVisible(Optional.empty(), Optional.of("00000000000"),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        verify(visitRepository, never()).findAll(Mockito.<Specification<Visit>>any());
    }

    @Test
    void searchVisible_filtersInaccessible() {
        Visit hidden = new Visit();
        Patient p2 = new Patient();
        p2.setId(UUID.randomUUID());
        hidden.setPatient(p2);

        when(visitRepository.findAll(Mockito.<Specification<Visit>>any())).thenReturn(List.of(visit, hidden));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(authorizationService.canReadPatient(doctor, p2)).thenReturn(false);

        Page<VisitDto> page;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            page = service.searchVisible(Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), PageRequest.of(0, 10));
        }

        assertThat(page.getContent()).hasSize(1);
    }

    // ── exportIcs ────────────────────────────────────────────────────────

    @Test
    void exportIcs_authorizedConfirmedVisit_returnsValidIcs() {
        visit.setStatus(Visit.Status.CONFIRMED);
        visit.setConfidential(false);
        visit.setDiagnosis("Grypa");
        when(visitRepository.findByIdWithPatient(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<String> ics;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ics = service.exportIcs(visit.getId());
        }

        assertThat(ics).isPresent();
        assertThat(ics.get()).contains("BEGIN:VCALENDAR")
                .contains("BEGIN:VEVENT")
                .contains("STATUS:CONFIRMED")
                .contains("SUMMARY:Konsultacja")
                .contains("Diagnoza: Grypa")
                .contains("END:VEVENT")
                .contains("END:VCALENDAR");
    }

    @Test
    void exportIcs_confidentialVisit_redactsDescription() {
        visit.setStatus(Visit.Status.PLANNED);
        visit.setConfidential(true);
        visit.setReason("Tajne");
        when(visitRepository.findByIdWithPatient(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<String> ics;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ics = service.exportIcs(visit.getId());
        }

        assertThat(ics).isPresent();
        assertThat(ics.get()).contains("STATUS:TENTATIVE")
                .contains("Szczegóły poufne")
                .doesNotContain("Tajne");
    }

    @Test
    void exportIcs_canceledStatus_mapsToCancelled() {
        visit.setStatus(Visit.Status.CANCELED);
        when(visitRepository.findByIdWithPatient(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        Optional<String> ics;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ics = service.exportIcs(visit.getId());
        }

        assertThat(ics.get()).contains("STATUS:CANCELLED");
    }

    @Test
    void exportIcs_unauthorized_returnsEmpty() {
        when(visitRepository.findByIdWithPatient(visit.getId())).thenReturn(Optional.of(visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(false);

        Optional<String> ics;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ics = service.exportIcs(visit.getId());
        }

        assertThat(ics).isEmpty();
    }

    @Test
    void exportCalendarIcs_multipleVisits_returnsAggregatedCalendar() {
        Visit v2 = new Visit();
        v2.setId(UUID.randomUUID());
        v2.setPatient(patient);
        v2.setDoctor(doctor);
        v2.setVisitDate(start.plus(1, ChronoUnit.HOURS));
        v2.setEndDate(start.plus(1, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES));
        v2.setStatus(Visit.Status.PLANNED);
        v2.setVisitType("Kontrola");

        when(visitRepository.findAll(Mockito.<Specification<Visit>>any())).thenReturn(List.of(v2, visit));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);

        String ics;
        try (MockedStatic<SecurityUtil> m = mockCurrent(doctor)) {
            ics = service.exportCalendarIcs(Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty());
        }

        // Expect two events, sorted by visit date (v2 has later start)
        assertThat(ics).startsWith("BEGIN:VCALENDAR");
        assertThat(ics.split("BEGIN:VEVENT")).hasSize(3); // first split before any event
        int firstUid = ics.indexOf("UID:" + visit.getId());
        int secondUid = ics.indexOf("UID:" + v2.getId());
        assertThat(firstUid).isLessThan(secondUid);
    }
}

