package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.PatientDto;
import com.example.emr_server.controller.dto.request.PatientCreateRequest;
import com.example.emr_server.controller.dto.request.PatientUpdateRequest;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.service.PatientService;
import com.example.emr_server.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pacjenci")
@Tag(name = "Pacjenci", description = "Zarządzanie pacjentami")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Pobierz pacjentów (widocznych) – z filtrami + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<PatientDto>> getAllPacjenci(
            @RequestParam Optional<String> firstName,
            @RequestParam Optional<String> lastName,
            @RequestParam Optional<String> pesel,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<java.time.LocalDate> dobStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<java.time.LocalDate> dobEnd,
            @RequestParam Optional<String> gender,
            @RequestParam Optional<String> addressFragment,
            @RequestParam Optional<java.util.UUID> createdById,
            Pageable pageable
    ) {
        Page<PatientDto> page = patientService.searchVisible(
                firstName, lastName, pesel, dobStart, dobEnd, gender, addressFragment, createdById, pageable
        );
        return ResponseEntity.ok(page);
    }

    // Pobierz pacjenta po ID (DTO, bez wycieku lazy asocjacji)
    @GetMapping("/{id}")
    public ResponseEntity<PatientDto> getPacjentById(@PathVariable UUID id) {
        return patientService.getVisibleById(id)
                .map(ResponseEntity::ok)
                // 404 buduje się lepiej dla braku widoczności (nie ujawniaj istnienia)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // Utwórz nowego pacjenta (DTO)
    @PostMapping
    @Operation(summary = "Utwórz pacjenta", description = "Tworzy pacjenta na podstawie PatientCreateRequest")
    public ResponseEntity<PatientDto> createPacjent(@Valid @RequestBody PatientCreateRequest req) {
        try {
            Patient p = new Patient();
            p.setFirstName(req.firstName());
            p.setLastName(req.lastName());
            p.setPesel(req.pesel());
            p.setDateOfBirth(req.dateOfBirth());
            p.setGender(req.gender());
            p.setContactInfo(req.contactInfo());
            p.setAddress(req.address());
            PatientDto saved = patientService.createForCurrent(p);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    // Zaktualizuj dane pacjenta (DTO)
    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj pacjenta", description = "Aktualizuje pacjenta na podstawie PatientUpdateRequest")
    public ResponseEntity<PatientDto> updatePacjent(@PathVariable UUID id, @Valid @RequestBody PatientUpdateRequest req) {
        try {
            Patient update = new Patient();
            update.setFirstName(req.firstName());
            update.setLastName(req.lastName());
            update.setPesel(req.pesel());
            update.setDateOfBirth(req.dateOfBirth());
            update.setGender(req.gender());
            update.setContactInfo(req.contactInfo());
            update.setAddress(req.address());
            return patientService.updateForCurrent(id, update)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    // Usuń pacjenta
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePacjent(@PathVariable UUID id) {
        try {
            boolean deleted = patientService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
