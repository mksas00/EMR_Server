package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.LabResultDto;
import com.example.emr_server.controller.dto.request.LabResultCreateRequest;
import com.example.emr_server.controller.dto.request.LabResultUpdateRequest;
import com.example.emr_server.entity.LabResult;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.service.LabResultService;
import com.example.emr_server.util.PageUtils;
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
@RequestMapping("/api/wyniki-lab")
@Tag(name = "Wyniki laboratoryjne", description = "Zarządzanie wynikami badań lab")
public class LabResultController {

    private final LabResultService labResultService;

    public LabResultController(LabResultService labResultService) {
        this.labResultService = labResultService;
    }

    // GET /api/wyniki-lab – filtry AND + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<LabResultDto>> list(
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<UUID> orderedById,
            @RequestParam Optional<String> status,
            @RequestParam Optional<LocalDate> start,
            @RequestParam Optional<LocalDate> end,
            @RequestParam Optional<String> testName,
            @RequestParam Optional<String> resultFragment,
            Pageable pageable
    ) {
        Page<LabResultDto> page = labResultService.searchVisible(
                patientId, orderedById, status, start, end, testName, resultFragment, pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabResultDto> get(@PathVariable UUID id) {
        return labResultService.getVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping
    @Operation(summary = "Utwórz wynik badania", description = "Tworzy wynik na podstawie LabResultCreateRequest")
    public ResponseEntity<LabResultDto> create(@Valid @RequestBody LabResultCreateRequest req) {
        try {
            LabResult lr = new LabResult();
            Patient p = new Patient();
            p.setId(req.patientId());
            lr.setPatient(p);
            lr.setTestName(req.testName());
            lr.setResult(req.result());
            lr.setResultDate(req.resultDate());
            lr.setUnit(req.unit());
            lr.setReferenceRange(req.referenceRange());
            lr.setStatus(req.status());
            LabResultDto dto = labResultService.createForCurrent(lr);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj wynik badania", description = "Aktualizuje wynik na podstawie LabResultUpdateRequest")
    public ResponseEntity<LabResultDto> update(@PathVariable UUID id, @Valid @RequestBody LabResultUpdateRequest req) {
        try {
            LabResult update = new LabResult();
            update.setTestName(req.testName());
            update.setResult(req.result());
            update.setResultDate(req.resultDate());
            update.setUnit(req.unit());
            update.setReferenceRange(req.referenceRange());
            update.setStatus(req.status());
            return labResultService.updateForCurrent(id, update)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            boolean deleted = labResultService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
