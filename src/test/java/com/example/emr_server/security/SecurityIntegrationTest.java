package com.example.emr_server.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.max-json-bytes=100",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Nagłówki bezpieczeństwa na /actuator/health")
    void securityHeadersOnHealth() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Cross-Origin-Opener-Policy", "same-origin"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    @DisplayName("CORS preflight dla dozwolonego originu")
    void corsPreflightAllowedOrigin() throws Exception {
        mvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Vary", org.hamcrest.Matchers.containsString("Origin")));
    }

    @Test
    @DisplayName("Limit rozmiaru JSON zwraca 413")
    void jsonRequestSizeLimit() throws Exception {
        // Given: JSON request larger than 100 bytes limit
        String bigJson = "{\"a\":\"" + "x".repeat(200) + "\"}";

        // When & Then: request rejected with 413 Payload Too Large
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bigJson))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @DisplayName("Brak autoryzacji -> 401 na chronionym zasobie")
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }
}
