package com.example.emr_server.service;

import com.example.emr_server.controller.dto.PatientConsentDto;
import com.example.emr_server.controller.dto.request.ConsentGrantRequest;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PatientConsentRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

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
 * Unit tests for {@link PatientConsentServiceImpl}.
 *
 * Covered:
 *  grant – happy path (new consent), idempotency (revoke old + create new),
 *          forbidden (no write access), self-grant, missing patient/user,
 *          past expiresAt
 *  revoke – happy path, already revoked (conflict), forbidden
 *  listForPatient – delegates to repo, checks authorization
 *  listMyActiveConsents – delegates to repo with userId + now
 */
@ExtendWith(MockitoExtension.class)
class PatientConsentServiceImplTest {

    @Mock private PatientConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PatientService patientService;
    @Mock private AuthorizationService authorizationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private PatientConsentServiceImpl service;

    private User doctor;
    private User nurse;
    private Patient patient;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setUsername("doctor1");
        doctor.setRole("doctor");

        nurse = new User();
        nurse.setId(UUID.randomUUID());
        nurse.setUsername("nurse1");
        nurse.setRole("nurse");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("Jan");
        patient.setLastName("Kowalski");
    }

    // ── grant ─────────────────────────────────────────────────────────────

    @Test
    void grant_validRequest_savesConsentAndAudits() {
        // Given
        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), nurse.getId(), "read", "Konsultacja", null);

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(userRepository.findById(nurse.getId())).thenReturn(Optional.of(nurse));
        when(consentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, nurse, "read"))
                .thenReturn(Optional.empty());
        when(consentRepository.save(any(PatientConsent.class))).thenAnswer(inv -> {
            PatientConsent c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });

        // When
        PatientConsentDto dto;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            dto = service.grant(req);
        }

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.scope()).isEqualTo("read");
        assertThat(dto.grantedToUsername()).isEqualTo("nurse1");
        assertThat(dto.active()).isTrue();
        assertThat(dto.revokedAt()).isNull();

        ArgumentCaptor<PatientConsent> captor = ArgumentCaptor.forClass(PatientConsent.class);
        verify(consentRepository).save(captor.capture());
        PatientConsent saved = captor.getValue();
        assertThat(saved.getPatient()).isEqualTo(patient);
        assertThat(saved.getGrantedTo()).isEqualTo(nurse);
        assertThat(saved.getScope()).isEqualTo("read");
        assertThat(saved.getReason()).isEqualTo("Konsultacja");

        verify(auditService).logPatient(eq(doctor), eq(patient), eq("GRANT_CONSENT"), anyString());
    }

    @Test
    void grant_withExpiresAt_setsExpiryOnConsent() {
        // Given
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), nurse.getId(), "read", "Tymczasowy dostęp", expiresAt);

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(userRepository.findById(nurse.getId())).thenReturn(Optional.of(nurse));
        when(consentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, nurse, "read"))
                .thenReturn(Optional.empty());
        when(consentRepository.save(any(PatientConsent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            PatientConsentDto dto = service.grant(req);
            // Then
            assertThat(dto.expiresAt()).isEqualTo(expiresAt);
        }
    }

    @Test
    void grant_existingActiveConsent_revokesOldAndCreatesNew() {
        // Given: istnieje już aktywna zgoda read – powinna zostać cofnięta
        PatientConsent existing = new PatientConsent();
        existing.setId(UUID.randomUUID());
        existing.setPatient(patient);
        existing.setGrantedTo(nurse);
        existing.setScope("read");
        existing.setGrantedAt(Instant.now().minus(10, ChronoUnit.DAYS));

        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), nurse.getId(), "read", "Odnowienie", null);

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(userRepository.findById(nurse.getId())).thenReturn(Optional.of(nurse));
        when(consentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, nurse, "read"))
                .thenReturn(Optional.of(existing));
        when(consentRepository.save(any(PatientConsent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            service.grant(req);
        }

        // Then: stara zgoda cofnięta (save wywołane 2x: raz dla old, raz dla new)
        assertThat(existing.getRevokedAt()).isNotNull();
        verify(consentRepository, Mockito.times(2)).save(any(PatientConsent.class));
    }

    @Test
    void grant_selfGrant_throwsBadRequest() {
        // Given: doctor próbuje nadać dostęp samemu sobie
        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), doctor.getId(), "read", null, null);

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.grant(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("samemu sobie");
        }
        verify(consentRepository, never()).save(any());
    }

    @Test
    void grant_noWriteAccess_throwsForbidden() {
        // Given
        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), nurse.getId(), "read", null, null);

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(false);

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.grant(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Nie masz uprawnień");
        }
        verify(consentRepository, never()).save(any());
    }

    @Test
    void grant_patientNotFound_throwsNotFound() {
        // Given
        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), nurse.getId(), "read", null, null);
        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.empty());

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.grant(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Pacjent nie istnieje");
        }
    }

    @Test
    void grant_grantedUserNotFound_throwsNotFound() {
        // Given
        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), nurse.getId(), "read", null, null);

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(userRepository.findById(nurse.getId())).thenReturn(Optional.empty());

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.grant(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Użytkownik nie istnieje");
        }
    }

    @Test
    void grant_pastExpiresAt_throwsBadRequest() {
        // Given
        ConsentGrantRequest req = new ConsentGrantRequest(
                patient.getId(), nurse.getId(), "read", null,
                Instant.now().minus(1, ChronoUnit.HOURS));

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(userRepository.findById(nurse.getId())).thenReturn(Optional.of(nurse));

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.grant(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("przeszłości");
        }
        verify(consentRepository, never()).save(any());
    }

    // ── revoke ────────────────────────────────────────────────────────────

    @Test
    void revoke_activeConsent_setsRevokedAtAndAudits() {
        // Given
        PatientConsent consent = makeConsent(null, "read");

        when(consentRepository.findById(consent.getId())).thenReturn(Optional.of(consent));
        when(authorizationService.canWritePatient(doctor, patient)).thenReturn(true);
        when(consentRepository.save(any(PatientConsent.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PatientConsentDto dto;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            dto = service.revoke(consent.getId());
        }

        // Then
        assertThat(dto.revokedAt()).isNotNull();
        assertThat(dto.active()).isFalse();
        verify(auditService).logPatient(eq(doctor), eq(patient), eq("REVOKE_CONSENT"), anyString());
        verify(consentRepository).save(consent);
    }

    @Test
    void revoke_alreadyRevoked_throwsConflict() {
        // Given
        PatientConsent consent = makeConsent(Instant.now().minus(1, ChronoUnit.HOURS), "read");

        when(consentRepository.findById(consent.getId())).thenReturn(Optional.of(consent));

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.revoke(consent.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("już cofnięta");
        }
        verify(consentRepository, never()).save(any());
    }

    @Test
    void revoke_noAccess_throwsForbidden() {
        // Given: nurse próbuje cofnąć zgodę, do której nie ma write access
        PatientConsent consent = makeConsent(null, "read");

        when(consentRepository.findById(consent.getId())).thenReturn(Optional.of(consent));
        when(authorizationService.canWritePatient(nurse, patient)).thenReturn(false);

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(nurse));
            assertThatThrownBy(() -> service.revoke(consent.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Brak uprawnień do cofnięcia");
        }
        verify(consentRepository, never()).save(any());
    }

    @Test
    void revoke_adminCanRevokeAnyConsent() {
        // Given: admin bez eksplicytnego canWritePatient może cofnąć
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole("admin");
        admin.setUsername("admin");

        PatientConsent consent = makeConsent(null, "read");
        when(consentRepository.findById(consent.getId())).thenReturn(Optional.of(consent));
        when(consentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(admin));
            PatientConsentDto dto = service.revoke(consent.getId());
            // Then
            assertThat(dto.revokedAt()).isNotNull();
        }
        verify(authorizationService, never()).canWritePatient(any(), any());
    }

    @Test
    void revoke_consentNotFound_throwsNotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(consentRepository.findById(id)).thenReturn(Optional.empty());

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            assertThatThrownBy(() -> service.revoke(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Zgoda nie istnieje");
        }
    }

    // ── listForPatient ────────────────────────────────────────────────────

    @Test
    void listForPatient_authorizedDoctor_returnsPage() {
        // Given
        PatientConsent c1 = makeConsent(null, "read");
        Page<PatientConsent> page = new PageImpl<>(List.of(c1));

        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canReadPatient(doctor, patient)).thenReturn(true);
        when(consentRepository.findByPatient_IdOrderByGrantedAtDesc(eq(patient.getId()), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<PatientConsentDto> result;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(doctor));
            result = service.listForPatient(patient.getId(), Pageable.unpaged());
        }

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).scope()).isEqualTo("read");
    }

    @Test
    void listForPatient_unauthorizedUser_throwsForbidden() {
        // Given
        when(patientService.findPatientById(patient.getId())).thenReturn(Optional.of(patient));
        when(authorizationService.canReadPatient(nurse, patient)).thenReturn(false);

        // When / Then
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(nurse));
            assertThatThrownBy(() -> service.listForPatient(patient.getId(), Pageable.unpaged()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Brak dostępu do pacjenta");
        }
        verify(consentRepository, never()).findByPatient_IdOrderByGrantedAtDesc(any(), any());
    }

    // ── listMyActiveConsents ──────────────────────────────────────────────

    @Test
    void listMyActiveConsents_returnsOnlyActiveConsents() {
        // Given
        PatientConsent active = makeConsent(null, "read");
        Page<PatientConsent> page = new PageImpl<>(List.of(active));

        when(consentRepository.findActiveConsentsByGrantedTo(
                eq(nurse.getId()), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        Page<PatientConsentDto> result;
        try (MockedStatic<SecurityUtil> m = Mockito.mockStatic(SecurityUtil.class)) {
            m.when(() -> SecurityUtil.getCurrentUser(userRepository)).thenReturn(Optional.of(nurse));
            result = service.listMyActiveConsents(Pageable.unpaged());
        }

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).active()).isTrue();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private PatientConsent makeConsent(Instant revokedAt, String scope) {
        PatientConsent c = new PatientConsent();
        c.setId(UUID.randomUUID());
        c.setPatient(patient);
        c.setGrantedTo(nurse);
        c.setScope(scope);
        c.setGrantedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        c.setRevokedAt(revokedAt);
        return c;
    }
}

