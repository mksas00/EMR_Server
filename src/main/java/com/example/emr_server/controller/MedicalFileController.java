package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.MedicalFileDto;
import com.example.emr_server.controller.dto.request.MedicalFileCreateRequest;
import com.example.emr_server.controller.dto.request.MedicalFileUpdateRequest;
import com.example.emr_server.entity.MedicalFile;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.service.MedicalFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pliki-medyczne")
@Tag(name = "Pliki medyczne", description = "Zarządzanie plikami medycznymi")
public class MedicalFileController {

    private final MedicalFileService medicalFileService;

    public MedicalFileController(MedicalFileService medicalFileService) {
        this.medicalFileService = medicalFileService;
    }

    // GET /api/pliki-medyczne – filtry AND + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<MedicalFileDto>> list(
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<UUID> uploadedById,
            @RequestParam Optional<String> mimeType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> end,
            @RequestParam Optional<String> fileNameFragment,
            Pageable pageable
    ) {
        Page<MedicalFileDto> page = medicalFileService.searchVisible(patientId, uploadedById, mimeType, start, end, fileNameFragment, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalFileDto> get(@PathVariable UUID id) {
        return medicalFileService.getVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping
    @Operation(summary = "Utwórz plik medyczny", description = "Tworzy plik na podstawie MedicalFileCreateRequest")
    public ResponseEntity<MedicalFileDto> create(@Valid @RequestBody MedicalFileCreateRequest req) {
        try {
            MedicalFile file = new MedicalFile();
            Patient p = new Patient();
            p.setId(req.patientId());
            file.setPatient(p);
            file.setFileName(req.fileName());
            file.setFilePath(req.filePath());
            file.setMimeType(req.mimeType());
            MedicalFileDto dto = medicalFileService.createForCurrent(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj plik medyczny", description = "Aktualizuje plik na podstawie MedicalFileUpdateRequest")
    public ResponseEntity<MedicalFileDto> update(@PathVariable UUID id, @Valid @RequestBody MedicalFileUpdateRequest req) {
        try {
            MedicalFile update = new MedicalFile();
            update.setFileName(req.fileName());
            update.setFilePath(req.filePath());
            update.setMimeType(req.mimeType());
            return medicalFileService.updateForCurrent(id, update)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            boolean deleted = medicalFileService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
