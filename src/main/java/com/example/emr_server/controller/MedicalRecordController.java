package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.MedicalRecordDto;
import com.example.emr_server.controller.dto.request.MedicalRecordCreateRequest;
import com.example.emr_server.controller.dto.request.MedicalRecordUpdateRequest;
import com.example.emr_server.entity.MedicalRecord;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.service.MedicalRecordService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/rekordy")
@Tag(name = "Rekordy medyczne", description = "Zarządzanie rekordami medycznymi")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    // GET /api/rekordy – filtry AND + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<MedicalRecordDto>> list(
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<UUID> createdById,
            @RequestParam Optional<String> type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> end,
            Pageable pageable
    ) {
        Page<MedicalRecordDto> page = medicalRecordService.searchVisible(patientId, createdById, type, start, end, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordDto> get(@PathVariable UUID id) {
        return medicalRecordService.getVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping
    @Operation(summary = "Utwórz rekord medyczny", description = "Tworzy rekord na podstawie MedicalRecordCreateRequest")
    public ResponseEntity<MedicalRecordDto> create(@Valid @RequestBody MedicalRecordCreateRequest req) {
        try {
            MedicalRecord mr = new MedicalRecord();
            Patient p = new Patient();
            p.setId(req.patientId());
            mr.setPatient(p);
            mr.setRecordType(req.recordType());
            mr.setContent(req.content());
            mr.setIsEncrypted(req.isEncrypted());
            mr.setEncryptedChecksum(req.encryptedChecksum());
            MedicalRecordDto dto = medicalRecordService.createForCurrent(mr);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj rekord medyczny", description = "Aktualizuje rekord na podstawie MedicalRecordUpdateRequest")
    public ResponseEntity<MedicalRecordDto> update(@PathVariable UUID id, @Valid @RequestBody MedicalRecordUpdateRequest req) {
        try {
            MedicalRecord update = new MedicalRecord();
            update.setRecordType(req.recordType());
            update.setContent(req.content());
            update.setIsEncrypted(req.isEncrypted());
            update.setEncryptedChecksum(req.encryptedChecksum());
            return medicalRecordService.updateForCurrent(id, update)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            boolean deleted = medicalRecordService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
