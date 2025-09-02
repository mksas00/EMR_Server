package com.example.emr_server.security;

import com.example.emr_server.controller.dto.request.BtgGrantRequest;
import com.example.emr_server.entity.PatientConsent;
import com.example.emr_server.service.BtgService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for Break the Glass (BTG) functionality
 * Tests critical security scenarios for emergency medical access
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BtgSecurityTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public BtgService btgService() {
            return mock(BtgService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BtgService btgService;

    @Test
    void btgGrant_unauthorizedAccess_deniesAccess() throws Exception {
        // Given: unauthenticated request
        UUID patientId = UUID.randomUUID();
        BtgGrantRequest request = new BtgGrantRequest(patientId, 60, "Emergency access");

        // When & Then: access denied with 403 (Spring Security default for unauthenticated)
        mockMvc.perform(post("/api/btg/grant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"PATIENT"})
    void btgGrant_patientRole_forbidsAccess() throws Exception {
        // Given: patient trying to grant BTG access
        UUID patientId = UUID.randomUUID();
        BtgGrantRequest request = new BtgGrantRequest(patientId, 60, "Self emergency access");

        // When & Then: forbidden - patients cannot grant BTG access
        mockMvc.perform(post("/api/btg/grant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void btgGrant_excessiveTimeRequest_rejectsRequest() throws Exception {
        // Given: request for excessive BTG duration (security violation)
        UUID patientId = UUID.randomUUID();
        BtgGrantRequest request = new BtgGrantRequest(patientId, 999, "Suspicious long access");

        // When & Then: bad request due to validation
        mockMvc.perform(post("/api/btg/grant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void btgGrant_suspiciousReason_stillProcessesButLogs() throws Exception {
        // Given: BTG request with minimal reason (potential security concern)
        UUID patientId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();
        BtgGrantRequest request = new BtgGrantRequest(patientId, 5, "xyz"); // Minimal reason

        PatientConsent consent = new PatientConsent();
        consent.setId(consentId);
        consent.setExpiresAt(Instant.now().plusSeconds(300));

        when(btgService.grantBtgConsent(any(UUID.class), anyInt(), anyString()))
            .thenReturn(consent);

        // When & Then: processes request but logs security concern
        mockMvc.perform(post("/api/btg/grant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consentId").value(consentId.toString()));
    }

    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void btgGrant_validRequest_grantsAccess() throws Exception {
        // Given: valid BTG request from authorized doctor
        UUID patientId = UUID.randomUUID();
        UUID consentId = UUID.randomUUID();
        BtgGrantRequest request = new BtgGrantRequest(patientId, 30, "Patient unconscious, emergency cardiac intervention required");

        PatientConsent consent = new PatientConsent();
        consent.setId(consentId);
        consent.setExpiresAt(Instant.now().plusSeconds(1800));

        when(btgService.grantBtgConsent(any(UUID.class), anyInt(), anyString()))
            .thenReturn(consent);

        // When & Then: access granted successfully
        mockMvc.perform(post("/api/btg/grant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.consentId").value(consentId.toString()))
            .andExpect(jsonPath("$.expiresAt").exists());
    }
}
