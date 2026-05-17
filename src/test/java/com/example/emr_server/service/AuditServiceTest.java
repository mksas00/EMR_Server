package com.example.emr_server.service;

import com.example.emr_server.entity.AuditLog;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AuditService}.
 * Covers: logPatient persistence with all fields, log(...) swallowing repository
 * failures, and diffPatient comparison logic (PESEL masking, address redaction,
 * field-level change formatting).
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private User user;
    private Patient patient;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("doc");

        patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setFirstName("Jan");
        patient.setLastName("Kowalski");
        patient.setPesel("12345678901");
    }

    // ── logPatient ───────────────────────────────────────────────────────

    @Test
    void logPatient_validInputs_persistsAuditEntryWithAllFields() {
        // When
        auditService.logPatient(user, patient, "CREATE_PATIENT", "desc");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getPatient()).isEqualTo(patient);
        assertThat(saved.getAction()).isEqualTo("CREATE_PATIENT");
        assertThat(saved.getDescription()).isEqualTo("desc");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void logPatient_nullPatient_stillPersistsEntry() {
        // Given / When
        auditService.logPatient(user, null, "LOGIN", "user login");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getPatient()).isNull();
        assertThat(captor.getValue().getAction()).isEqualTo("LOGIN");
    }

    // ── log (generic) ────────────────────────────────────────────────────

    @Test
    void log_validInputs_persistsAuditEntryWithoutPatient() {
        // When
        auditService.log(user, "USER_LOGIN", "Login success");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getPatient()).isNull();
        assertThat(saved.getAction()).isEqualTo("USER_LOGIN");
        assertThat(saved.getDescription()).isEqualTo("Login success");
    }

    @Test
    void log_repositoryThrows_doesNotPropagateException() {
        // Given: repository fails (e.g. RLS or constraint)
        doThrow(new RuntimeException("db down")).when(auditLogRepository).save(any(AuditLog.class));

        // When / Then: no exception propagated (silent best-effort)
        auditService.log(user, "X", "y"); // should not throw
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ── diffPatient ──────────────────────────────────────────────────────

    @Test
    void diffPatient_bothNull_returnsEmptyString() {
        assertThat(auditService.diffPatient(null, null)).isEmpty();
    }

    @Test
    void diffPatient_oneNull_returnsEmptyString() {
        assertThat(auditService.diffPatient(patient, null)).isEmpty();
        assertThat(auditService.diffPatient(null, patient)).isEmpty();
    }

    @Test
    void diffPatient_noChanges_returnsEmptyString() {
        Patient copy = new Patient();
        copy.setId(patient.getId());
        copy.setFirstName(patient.getFirstName());
        copy.setLastName(patient.getLastName());
        copy.setPesel(patient.getPesel());

        assertThat(auditService.diffPatient(patient, copy)).isEmpty();
    }

    @Test
    void diffPatient_firstNameChanged_reportsFieldDiff() {
        Patient after = new Patient();
        after.setFirstName("Adam");
        after.setLastName(patient.getLastName());
        after.setPesel(patient.getPesel());

        String diff = auditService.diffPatient(patient, after);

        assertThat(diff).contains("firstName: 'Jan'->'Adam'");
    }

    @Test
    void diffPatient_peselChanged_masksValuesInOutput() {
        Patient after = new Patient();
        after.setFirstName(patient.getFirstName());
        after.setLastName(patient.getLastName());
        after.setPesel("99999999912");

        String diff = auditService.diffPatient(patient, after);

        // PESEL is masked: keep only last 4 digits
        assertThat(diff).contains("pesel:")
                .contains("***8901")
                .contains("***9912")
                .doesNotContain("12345678901")
                .doesNotContain("99999999912");
    }

    @Test
    void diffPatient_addressChanged_omitsValuesFromDiffByDesign() {
        // Address is intentionally redacted to identical placeholder; therefore the diff
        // does not leak the actual change content (privacy-by-design).
        patient.setAddress("ul. Stara 1");
        Patient after = new Patient();
        after.setFirstName(patient.getFirstName());
        after.setLastName(patient.getLastName());
        after.setPesel(patient.getPesel());
        after.setAddress("ul. Nowa 5");

        String diff = auditService.diffPatient(patient, after);

        assertThat(diff).doesNotContain("Stara").doesNotContain("Nowa");
    }

    @Test
    void diffPatient_contactInfoChanged_omitsMapContentsByDesign() {
        // Same rationale as address – contactInfo is replaced by an opaque "{...}" token
        // so the diff cannot leak phone / email values.
        patient.setContactInfo(new HashMap<>(Map.of("phone", "111")));
        Patient after = new Patient();
        after.setFirstName(patient.getFirstName());
        after.setLastName(patient.getLastName());
        after.setPesel(patient.getPesel());
        after.setContactInfo(new HashMap<>(Map.of("phone", "222")));

        String diff = auditService.diffPatient(patient, after);

        assertThat(diff).doesNotContain("111").doesNotContain("222");
    }

    @Test
    void diffPatient_dobAndGenderChanges_reportedDirectly() {
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender("M");
        Patient after = new Patient();
        after.setFirstName(patient.getFirstName());
        after.setLastName(patient.getLastName());
        after.setPesel(patient.getPesel());
        after.setDateOfBirth(LocalDate.of(1991, 2, 2));
        after.setGender("F");

        String diff = auditService.diffPatient(patient, after);

        assertThat(diff).contains("dateOfBirth:")
                .contains("1990-01-01")
                .contains("1991-02-02")
                .contains("gender:")
                .contains("'M'->'F'");
    }

    @Test
    void diffPatient_multipleChanges_returnsCommaSeparatedList() {
        Patient after = new Patient();
        after.setFirstName("Adam");
        after.setLastName("Nowak");
        after.setPesel(patient.getPesel());

        String diff = auditService.diffPatient(patient, after);

        assertThat(diff).contains("firstName:").contains("lastName:");
        assertThat(diff.split(", ")).hasSize(2);
    }
}

