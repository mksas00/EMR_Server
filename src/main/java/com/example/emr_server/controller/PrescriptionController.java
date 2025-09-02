package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.PrescriptionDto;
import com.example.emr_server.controller.dto.request.PrescriptionCreateRequest;
import com.example.emr_server.controller.dto.request.PrescriptionUpdateRequest;
import com.example.emr_server.controller.dto.request.PrescriptionItemRequest;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.Prescription;
import com.example.emr_server.entity.PrescriptionMedication;
import com.example.emr_server.entity.Medication;
import com.example.emr_server.service.PrescriptionService;
import com.example.emr_server.service.PrescriptionPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/recepty")
@Tag(name = "Recepty", description = "Zarządzanie receptami")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionPdfService prescriptionPdfService;

    public PrescriptionController(PrescriptionService prescriptionService, PrescriptionPdfService prescriptionPdfService) {
        this.prescriptionService = prescriptionService;
        this.prescriptionPdfService = prescriptionPdfService;
    }

    // GET /api/recepty – filtry AND + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<PrescriptionDto>> list(
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<UUID> doctorId,
            @RequestParam Optional<Boolean> active,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> end,
            Pageable pageable
    ) {
        Page<PrescriptionDto> page = prescriptionService.searchVisible(patientId, doctorId, active, start, end, pageable);
        return ResponseEntity.ok(page);
    }

    // GET /api/recepty/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionDto> get(@PathVariable UUID id) {
        return prescriptionService.getVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // POST /api/recepty
    @PostMapping
    @Operation(summary = "Utwórz receptę", description = "Tworzy receptę z listą pozycji leków")
    public ResponseEntity<PrescriptionDto> create(@Valid @RequestBody PrescriptionCreateRequest req) {
        try {
            Prescription p = new Prescription();
            Patient patient = new Patient();
            patient.setId(req.patientId());
            p.setPatient(patient);
            p.setDosageInfo(req.dosageInfo());
            p.setIssuedDate(req.issuedDate());
            p.setExpirationDate(req.expirationDate());
            p.setIsRepeatable(req.isRepeatable());
            if (req.items()!=null) {
                for (PrescriptionItemRequest ir : req.items()) {
                    PrescriptionMedication item = new PrescriptionMedication();
                    if (ir.medicationId()!=null) { var m = new Medication(); m.setId(ir.medicationId()); item.setMedication(m); }
                    item.setDosageInfo(ir.dosageInfo());
                    item.setQuantity(ir.quantity());
                    item.setUnit(ir.unit());
                    p.addItem(item);
                }
            }
            PrescriptionDto dto = prescriptionService.createForCurrent(p);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    // PUT /api/recepty/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj receptę", description = "Aktualizuje receptę oraz jej pozycje leków")
    public ResponseEntity<PrescriptionDto> update(@PathVariable UUID id, @Valid @RequestBody PrescriptionUpdateRequest req) {
        try {
            Prescription update = new Prescription();
            update.setDosageInfo(req.dosageInfo());
            update.setIssuedDate(req.issuedDate());
            update.setExpirationDate(req.expirationDate());
            update.setIsRepeatable(req.isRepeatable());
            if (req.items()!=null) {
                java.util.List<PrescriptionMedication> items = new java.util.ArrayList<>();
                for (PrescriptionItemRequest ir : req.items()) {
                    PrescriptionMedication item = new PrescriptionMedication();
                    if (ir.medicationId()!=null) { var m = new Medication(); m.setId(ir.medicationId()); item.setMedication(m); }
                    item.setDosageInfo(ir.dosageInfo());
                    item.setQuantity(ir.quantity());
                    item.setUnit(ir.unit());
                    items.add(item);
                }
                update.setItems(items);
            }
            return prescriptionService.updateForCurrent(id, update)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    // DELETE /api/recepty/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            boolean deleted = prescriptionService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // GET /api/recepty/{id}/pdf – zwraca PDF recepty
    @GetMapping(value = "/{id}/pdf")
    @Operation(summary = "Pobierz receptę jako PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        return prescriptionPdfService.generatePdfForVisible(id)
                .map(bytes -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", "recepta-" + id + ".pdf");
                    return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
