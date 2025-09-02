package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.ChronicDiseaseDto;
import com.example.emr_server.controller.dto.request.ChronicDiseaseCreateRequest;
import com.example.emr_server.controller.dto.request.ChronicDiseaseUpdateRequest;
import com.example.emr_server.entity.ChronicDisease;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.service.ChronicDiseaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/choroby-przewlekle")
@Tag(name = "Choroby przewlekłe", description = "Zarządzanie chorobami przewlekłymi")
public class ChronicDiseaseController {

    private final ChronicDiseaseService chronicDiseaseService;

    public ChronicDiseaseController(ChronicDiseaseService chronicDiseaseService) {
        this.chronicDiseaseService = chronicDiseaseService;
    }

    // GET /api/choroby-przewlekle – filtry AND + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<ChronicDiseaseDto>> list(
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<String> name,
            @RequestParam Optional<LocalDate> start,
            @RequestParam Optional<LocalDate> end,
            @RequestParam Optional<String> notesFragment,
            Pageable pageable
    ) {
        Page<ChronicDiseaseDto> page = chronicDiseaseService.searchVisible(patientId, name, start, end, notesFragment, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChronicDiseaseDto> get(@PathVariable UUID id) {
        return chronicDiseaseService.getVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping
    @Operation(summary = "Utwórz chorobę przewlekłą", description = "Tworzy wpis choroby na podstawie ChronicDiseaseCreateRequest")
    public ResponseEntity<ChronicDiseaseDto> create(@Valid @RequestBody ChronicDiseaseCreateRequest req) {
        try {
            ChronicDisease d = new ChronicDisease();
            Patient p = new Patient();
            p.setId(req.patientId());
            d.setPatient(p);
            d.setDiseaseName(req.diseaseName());
            d.setDiagnosedDate(req.diagnosedDate());
            d.setNotes(req.notes());
            ChronicDiseaseDto dto = chronicDiseaseService.createForCurrent(d);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj chorobę przewlekłą", description = "Aktualizuje wpis choroby na podstawie ChronicDiseaseUpdateRequest")
    public ResponseEntity<ChronicDiseaseDto> update(@PathVariable UUID id, @Valid @RequestBody ChronicDiseaseUpdateRequest req) {
        try {
            ChronicDisease update = new ChronicDisease();
            update.setDiseaseName(req.diseaseName());
            update.setDiagnosedDate(req.diagnosedDate());
            update.setNotes(req.notes());
            return chronicDiseaseService.updateForCurrent(id, update)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            boolean deleted = chronicDiseaseService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
