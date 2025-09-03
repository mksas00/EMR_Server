package com.example.emr_server.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for InputValidationService
 * Tests input validation and security filtering functionality
 */
@ExtendWith(MockitoExtension.class)
class InputValidationServiceTest {

    @InjectMocks
    private InputValidationService inputValidationService;

    @Test
    @DisplayName("isSafeInput_validInput_returnsTrue")
    void isSafeInput_validInput_returnsTrue() {
        // Given: safe medical text
        String input = "Patient has diabetes and hypertension";

        // When: validating input
        boolean result = inputValidationService.isSafeInput(input);

        // Then: input is considered safe
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isSafeInput_emptyOrNullInput_returnsTrue")
    void isSafeInput_emptyOrNullInput_returnsTrue() {
        // Given / When / Then: empty and null inputs are safe
        assertThat(inputValidationService.isSafeInput(null)).isTrue();
        assertThat(inputValidationService.isSafeInput("")).isTrue();
        assertThat(inputValidationService.isSafeInput("   ")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "'; DROP TABLE patients; --",
        "SELECT * FROM users WHERE id = 1",
        "INSERT INTO logs VALUES ('hack')",
        "UPDATE patients SET name = 'hacked'",
        "DELETE FROM medical_records",
        "CREATE TABLE temp AS SELECT *",
        "ALTER TABLE users ADD COLUMN",
        "EXEC sp_executesql",
        "UNION SELECT password FROM"
    })
    @DisplayName("containsSqlInjection_maliciousQueries_returnsTrue")
    void containsSqlInjection_maliciousQueries_returnsTrue(String maliciousInput) {
        // Given: SQL injection attempt
        // When: checking for SQL injection
        boolean result = inputValidationService.containsSqlInjection(maliciousInput);

        // Then: injection detected
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert('xss')</script>",
        "</script>",
        "javascript:alert(1)",
        "vbscript:msgbox(1)",
        "onload=alert(1)",
        "onerror=alert(1)",
        "onclick=alert(1)",
        "onmouseover=alert(1)"
    })
    @DisplayName("containsXss_maliciousScripts_returnsTrue")
    void containsXss_maliciousScripts_returnsTrue(String maliciousInput) {
        // Given: XSS attempt
        // When: checking for XSS
        boolean result = inputValidationService.containsXss(maliciousInput);

        // Then: XSS detected
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "12345678903, true",   // Valid PESEL with correct checksum
        "85010112345, true",   // Valid PESEL with checksum
        "1234567890, false",   // Too short
        "123456789012, false", // Too long
        "abcdefghijk, false",  // Non-numeric
        "12345678900, false"   // Invalid checksum
    })
    @DisplayName("isValidPesel_variousInputs_validatesCorrectly")
    void isValidPesel_variousInputs_validatesCorrectly(String pesel, boolean expected) {
        // Given: PESEL input
        // When: validating PESEL
        boolean result = inputValidationService.isValidPesel(pesel);

        // Then: validation result matches expected
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("isValidPesel_nullInput_returnsFalse")
    void isValidPesel_nullInput_returnsFalse() {
        // Given: null PESEL
        String pesel = null;

        // When: validating null PESEL
        boolean result = inputValidationService.isValidPesel(pesel);

        // Then: validation fails
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sanitizeMedicalText_maliciousInput_removesDangerousCharacters")
    void sanitizeMedicalText_maliciousInput_removesDangerousCharacters() {
        // Given: input with dangerous characters
        String input = "Patient <script>alert('xss')</script> has \"condition\" & symptoms";

        // When: sanitizing medical text
        String result = inputValidationService.sanitizeMedicalText(input);

        // Then: dangerous characters removed
        assertThat(result).doesNotContain("<", ">", "\"", "'", "&");
        assertThat(result).contains("Patient", "has", "condition", "symptoms");
    }

    @Test
    @DisplayName("sanitizeMedicalText_tooLongInput_truncatesTo1000Characters")
    void sanitizeMedicalText_tooLongInput_truncatesTo1000Characters() {
        // Given: very long input (over 1000 characters)
        String longInput = "Patient diagnosis: " + "x".repeat(1200);

        // When: sanitizing long text
        String result = inputValidationService.sanitizeMedicalText(longInput);

        // Then: text truncated to 1000 characters
        assertThat(result).hasSizeLessThanOrEqualTo(1000);
        assertThat(result).startsWith("Patient diagnosis:");
    }

    @Test
    @DisplayName("sanitizeMedicalText_nullInput_returnsNull")
    void sanitizeMedicalText_nullInput_returnsNull() {
        // Given: null input
        String input = null;

        // When: sanitizing null input
        String result = inputValidationService.sanitizeMedicalText(input);

        // Then: returns null
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("isSafeMedicalText_validMedicalText_returnsTrue")
    void isSafeMedicalText_validMedicalText_returnsTrue() {
        // Given: valid medical text with safe characters
        String input = "Patient: John Doe, Age: 45, Diagnosis: Type-2 diabetes (controlled)";

        // When: checking if text is safe
        boolean result = inputValidationService.isSafeMedicalText(input);

        // Then: text is considered safe
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isSafeMedicalText_textTooLong_returnsFalse")
    void isSafeMedicalText_textTooLong_returnsFalse() {
        // Given: text longer than 1000 characters
        String input = "Patient diagnosis: " + "a".repeat(1200);

        // When: checking if text is safe
        boolean result = inputValidationService.isSafeMedicalText(input);

        // Then: text is not safe due to length
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateAndLog_maliciousInput_throwsSecurityException")
    void validateAndLog_maliciousInput_throwsSecurityException() {
        // Given: malicious input
        String maliciousInput = "<script>alert('hack')</script>";
        String context = "patient-form";

        // When / Then: security exception thrown
        assertThatThrownBy(() -> inputValidationService.validateAndLog(maliciousInput, context))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Invalid input detected");
    }

    @Test
    @DisplayName("validateAndLog_safeInput_noExceptionThrown")
    void validateAndLog_safeInput_noExceptionThrown() {
        // Given: safe input
        String safeInput = "Patient has chronic condition";
        String context = "medical-record";

        // When: validating safe input
        inputValidationService.validateAndLog(safeInput, context);

        // Then: no exception thrown
        // Test passes if no exception is thrown
    }
}
