package com.example.emr_server.service;

import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PatientConsentRepository;
import com.example.emr_server.repository.PatientRepository;
import com.example.emr_server.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BtgServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientConsentRepository patientConsentRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetails customUserDetails;

    @InjectMocks
    private BtgServiceImpl btgService;

    @Captor
    private ArgumentCaptor<PatientConsent> consentCaptor;

    // --- grantBtgConsent method tests ---

    @Test
    void grantBtgConsent_validRequest_createsConsentWithCorrectExpiry() {
        // Given: patient exists and user is authenticated
        UUID patientId = UUID.randomUUID();
        String reason = "Emergency cardiac arrest - immediate access required";
        int minutes = 60;

        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setUsername("doctor.smith");

        PatientConsent savedConsent = new PatientConsent();
        savedConsent.setId(UUID.randomUUID());
        savedConsent.setPatient(patient);
        savedConsent.setGrantedTo(currentUser);
        savedConsent.setScope("btg");
        savedConsent.setReason(reason);
        savedConsent.setExpiresAt(Instant.now().plus(minutes, ChronoUnit.MINUTES));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.empty());
        when(patientConsentRepository.save(any(PatientConsent.class))).thenReturn(savedConsent);

        SecurityContextHolder.setContext(securityContext);

        // When: granting BTG consent
        PatientConsent result = btgService.grantBtgConsent(patientId, minutes, reason);

        // Then: consent created with correct properties and expiry
        assertThat(result).isNotNull();
        assertThat(result.getPatient()).isEqualTo(patient);
        assertThat(result.getGrantedTo()).isEqualTo(currentUser);
        assertThat(result.getScope()).isEqualTo("btg");
        assertThat(result.getReason()).isEqualTo(reason);
        assertThat(result.getExpiresAt()).isNotNull();

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        verify(patientConsentRepository).save(consentCaptor.capture());

        PatientConsent capturedConsent = consentCaptor.getValue();
        assertThat(capturedConsent.getPatient()).isEqualTo(patient);
        assertThat(capturedConsent.getGrantedTo()).isEqualTo(currentUser);
        assertThat(capturedConsent.getScope()).isEqualTo("btg");
        assertThat(capturedConsent.getReason()).isEqualTo(reason);
        assertThat(capturedConsent.getExpiresAt()).isAfter(Instant.now());
        assertThat(capturedConsent.getExpiresAt()).isBefore(Instant.now().plus(minutes + 1, ChronoUnit.MINUTES));

        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void grantBtgConsent_minimumValidDuration_createsShortTermConsent() {
        // Given: minimum valid duration (1 minute)
        UUID patientId = UUID.randomUUID();
        String reason = "Immediate medication verification needed";
        int minutes = 1;

        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        PatientConsent savedConsent = new PatientConsent();
        savedConsent.setId(UUID.randomUUID());
        savedConsent.setExpiresAt(Instant.now().plus(minutes, ChronoUnit.MINUTES));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.empty());
        when(patientConsentRepository.save(any(PatientConsent.class))).thenReturn(savedConsent);

        SecurityContextHolder.setContext(securityContext);

        // When: granting minimum duration BTG consent
        PatientConsent result = btgService.grantBtgConsent(patientId, minutes, reason);

        // Then: short-term consent created successfully
        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        assertThat(result.getExpiresAt()).isBefore(Instant.now().plus(2, ChronoUnit.MINUTES));

        verify(patientConsentRepository).save(any(PatientConsent.class));
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void grantBtgConsent_maximumValidDuration_createsExtendedConsent() {
        // Given: maximum valid duration (120 minutes)
        UUID patientId = UUID.randomUUID();
        String reason = "Complex surgical procedure requiring extended emergency access";
        int minutes = 120;

        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        PatientConsent savedConsent = new PatientConsent();
        savedConsent.setId(UUID.randomUUID());
        savedConsent.setExpiresAt(Instant.now().plus(minutes, ChronoUnit.MINUTES));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.empty());
        when(patientConsentRepository.save(any(PatientConsent.class))).thenReturn(savedConsent);

        SecurityContextHolder.setContext(securityContext);

        // When: granting maximum duration BTG consent
        PatientConsent result = btgService.grantBtgConsent(patientId, minutes, reason);

        // Then: extended consent created successfully
        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(Instant.now().plus(119, ChronoUnit.MINUTES));
        assertThat(result.getExpiresAt()).isBefore(Instant.now().plus(121, ChronoUnit.MINUTES));

        verify(patientConsentRepository).save(any(PatientConsent.class));
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void grantBtgConsent_nonExistentPatient_throwsIllegalArgumentException() {
        // Given: patient does not exist
        UUID nonExistentPatientId = UUID.randomUUID();
        when(patientRepository.findById(nonExistentPatientId)).thenReturn(Optional.empty());

        // When & Then: exception thrown for non-existent patient
        assertThatThrownBy(() -> btgService.grantBtgConsent(nonExistentPatientId, 30, "Emergency"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Nie znaleziono pacjenta");

        verify(patientRepository).findById(nonExistentPatientId);
        verify(patientConsentRepository, never()).save(any());
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void grantBtgConsent_noUserInSecurityContext_throwsSecurityException() {
        // Given: patient exists but no authenticated user
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // When & Then: exception thrown for missing user context
        assertThatThrownBy(() -> btgService.grantBtgConsent(patientId, 30, "Emergency"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Brak użytkownika w kontekście");

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository, never()).save(any());
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    // --- getActiveBtgConsent method tests ---

    @Test
    void getActiveBtgConsent_activeConsentExists_returnsConsent() {
        // Given: patient exists with active BTG consent
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        PatientConsent activeConsent = new PatientConsent();
        activeConsent.setId(UUID.randomUUID());
        activeConsent.setPatient(patient);
        activeConsent.setGrantedTo(currentUser);
        activeConsent.setScope("btg");
        activeConsent.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        activeConsent.setRevokedAt(null);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.of(activeConsent));

        SecurityContextHolder.setContext(securityContext);

        // When: checking for active BTG consent
        Optional<PatientConsent> result = btgService.getActiveBtgConsent(patientId);

        // Then: active consent returned
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(activeConsent);
        assertThat(result.get().getExpiresAt()).isAfter(Instant.now());

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void getActiveBtgConsent_expiredConsent_returnsEmpty() {
        // Given: patient exists with expired BTG consent
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        PatientConsent expiredConsent = new PatientConsent();
        expiredConsent.setId(UUID.randomUUID());
        expiredConsent.setExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES)); // Expired 10 minutes ago
        expiredConsent.setRevokedAt(null);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.of(expiredConsent));

        SecurityContextHolder.setContext(securityContext);

        // When: checking for active BTG consent
        Optional<PatientConsent> result = btgService.getActiveBtgConsent(patientId);

        // Then: no active consent returned due to expiration
        assertThat(result).isEmpty();

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void getActiveBtgConsent_noConsent_returnsEmpty() {
        // Given: patient exists but no BTG consent
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.empty());

        SecurityContextHolder.setContext(securityContext);

        // When: checking for active BTG consent
        Optional<PatientConsent> result = btgService.getActiveBtgConsent(patientId);

        // Then: no consent found
        assertThat(result).isEmpty();

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void getActiveBtgConsent_nonExistentPatient_throwsIllegalArgumentException() {
        // Given: patient does not exist
        UUID nonExistentPatientId = UUID.randomUUID();
        when(patientRepository.findById(nonExistentPatientId)).thenReturn(Optional.empty());

        // When & Then: exception thrown for non-existent patient
        assertThatThrownBy(() -> btgService.getActiveBtgConsent(nonExistentPatientId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Nie znaleziono pacjenta");

        verify(patientRepository).findById(nonExistentPatientId);
        verify(patientConsentRepository, never()).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(any(), any(), any());
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    // --- revokeBtgConsent method tests ---

    @Test
    void revokeBtgConsent_activeConsentExists_revokesAndReturnsTrue() {
        // Given: patient exists with active BTG consent
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        PatientConsent activeConsent = new PatientConsent();
        activeConsent.setId(UUID.randomUUID());
        activeConsent.setPatient(patient);
        activeConsent.setGrantedTo(currentUser);
        activeConsent.setScope("btg");
        activeConsent.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        activeConsent.setRevokedAt(null);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.of(activeConsent));

        SecurityContextHolder.setContext(securityContext);

        // When: revoking BTG consent
        boolean result = btgService.revokeBtgConsent(patientId);

        // Then: consent revoked successfully
        assertThat(result).isTrue();

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        verify(patientConsentRepository).save(consentCaptor.capture());

        PatientConsent revokedConsent = consentCaptor.getValue();
        assertThat(revokedConsent.getRevokedAt()).isNotNull();
        assertThat(revokedConsent.getRevokedAt()).isBeforeOrEqualTo(Instant.now());

        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void revokeBtgConsent_noActiveConsent_returnsFalse() {
        // Given: patient exists but no active BTG consent
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.empty());

        SecurityContextHolder.setContext(securityContext);

        // When: attempting to revoke BTG consent
        boolean result = btgService.revokeBtgConsent(patientId);

        // Then: nothing to revoke
        assertThat(result).isFalse();

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        verify(patientConsentRepository, never()).save(any());
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void revokeBtgConsent_expiredConsent_returnsFalse() {
        // Given: patient exists with expired BTG consent
        UUID patientId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);

        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        PatientConsent expiredConsent = new PatientConsent();
        expiredConsent.setId(UUID.randomUUID());
        expiredConsent.setExpiresAt(Instant.now().minus(10, ChronoUnit.MINUTES)); // Expired
        expiredConsent.setRevokedAt(null);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(customUserDetails);
        when(customUserDetails.getDomainUser()).thenReturn(currentUser);
        when(patientConsentRepository.findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg"))
            .thenReturn(Optional.of(expiredConsent));

        SecurityContextHolder.setContext(securityContext);

        // When: attempting to revoke expired BTG consent
        boolean result = btgService.revokeBtgConsent(patientId);

        // Then: expired consent not revoked
        assertThat(result).isFalse();

        verify(patientRepository).findById(patientId);
        verify(patientConsentRepository).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(patient, currentUser, "btg");
        verify(patientConsentRepository, never()).save(any());
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }

    @Test
    void revokeBtgConsent_nonExistentPatient_throwsIllegalArgumentException() {
        // Given: patient does not exist
        UUID nonExistentPatientId = UUID.randomUUID();
        when(patientRepository.findById(nonExistentPatientId)).thenReturn(Optional.empty());

        // When & Then: exception thrown for non-existent patient
        assertThatThrownBy(() -> btgService.revokeBtgConsent(nonExistentPatientId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Nie znaleziono pacjenta");

        verify(patientRepository).findById(nonExistentPatientId);
        verify(patientConsentRepository, never()).findByPatientAndGrantedToAndScopeAndRevokedAtIsNull(any(), any(), any());
        verify(patientConsentRepository, never()).save(any());
        verifyNoMoreInteractions(patientRepository, patientConsentRepository);
    }
}
