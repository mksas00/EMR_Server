package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.AllergyDto;
import com.example.emr_server.controller.dto.request.AllergyCreateRequest;
import com.example.emr_server.controller.dto.request.AllergyUpdateRequest;
import com.example.emr_server.entity.Allergy;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.service.AllergyService;
import com.example.emr_server.util.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/alergie")
@Tag(name = "Alergie", description = "Zarządzanie alergiami pacjentów")
public class AllergyController {

    private final AllergyService allergyService;

    public AllergyController(AllergyService allergyService) {
        this.allergyService = allergyService;
    }

    // GET /api/alergie – filtry AND + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<AllergyDto>> list(
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<String> allergen,
            @RequestParam Optional<String> severity,
            @RequestParam Optional<UUID> notedById,
            Pageable pageable
    ) {
        Page<AllergyDto> page = allergyService.searchVisible(patientId, allergen, severity, notedById, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AllergyDto> get(@PathVariable UUID id) {
        return allergyService.getVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping
    @Operation(summary = "Utwórz alergię", description = "Tworzy alergię na podstawie AllergyCreateRequest")
    public ResponseEntity<AllergyDto> create(@Valid @RequestBody AllergyCreateRequest req) {
        try {
            Allergy a = new Allergy();
            Patient p = new Patient();
            p.setId(req.patientId());
            a.setPatient(p);
            a.setAllergen(req.allergen());
            a.setReaction(req.reaction());
            a.setSeverity(req.severity());
            AllergyDto dto = allergyService.createForCurrent(a);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj alergię", description = "Aktualizuje alergię na podstawie AllergyUpdateRequest")
    public ResponseEntity<AllergyDto> update(@PathVariable UUID id, @Valid @RequestBody AllergyUpdateRequest req) {
        try {
            Allergy update = new Allergy();
            update.setAllergen(req.allergen());
            update.setReaction(req.reaction());
            update.setSeverity(req.severity());
            return allergyService.updateForCurrent(id, update)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            boolean deleted = allergyService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
