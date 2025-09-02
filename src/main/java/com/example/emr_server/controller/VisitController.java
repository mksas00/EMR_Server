package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.VisitDto;
import com.example.emr_server.controller.dto.request.VisitCreateRequest;
import com.example.emr_server.controller.dto.request.VisitUpdateRequest;
import com.example.emr_server.entity.Visit;
import com.example.emr_server.service.VisitService;
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

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/wizyty")
@Tag(name = "Wizyty", description = "Zarządzanie wizytami")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    // Pobierz wszystkie wizyty (widoczne) – z filtrami + paginacja/sort
    @GetMapping
    public ResponseEntity<Page<VisitDto>> getAllVisits(
            @RequestParam Optional<java.util.UUID> patientId,
            @RequestParam Optional<java.util.UUID> doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<java.time.Instant> start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<java.time.Instant> end,
            @RequestParam Optional<String> type,
            @RequestParam Optional<String> diagnosis,
            @RequestParam Optional<String> reason,
            @RequestParam Optional<Boolean> confidential,
            @RequestParam Optional<String> status,
            Pageable pageable
    ) {
        Page<VisitDto> page = visitService.searchVisible(
                patientId, doctorId, start, end, type, diagnosis, reason, confidential, status, pageable
        );
        return ResponseEntity.ok(page);
    }

    // Pobierz wizytę po ID (DTO)
    @GetMapping("/{id}")
    public ResponseEntity<VisitDto> getVisitById(@PathVariable UUID id) {
        return visitService.getVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // Eksport ICS
    @GetMapping(value = "/{id}/ics", produces = {"text/calendar", "application/ics", "application/octet-stream"})
    @Operation(summary = "Eksport ICS", description = "Eksportuje wizytę jako plik ICS (iCalendar)")
    public ResponseEntity<byte[]> exportIcs(@PathVariable UUID id) {
        return visitService.exportIcs(id)
                .map(ics -> {
                    byte[] data = ics.getBytes(StandardCharsets.UTF_8);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(new MediaType("text", "calendar"));
                    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=visit-"+id+".ics");
                    return new ResponseEntity<>(data, headers, HttpStatus.OK);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Utwórz nową wizytę powiązaną z pacjentem (DTO)
    @PostMapping("/{patientId}")
    @Operation(summary = "Utwórz wizytę", description = "Tworzy wizytę dla pacjenta na podstawie VisitCreateRequest")
    public ResponseEntity<VisitDto> createVisit(@PathVariable UUID patientId, @Valid @RequestBody VisitCreateRequest req) {
        try {
            Visit v = new Visit();
            v.setVisitDate(req.visitDate());
            v.setEndDate(req.endDate());
            v.setVisitType(req.visitType());
            v.setReason(req.reason());
            v.setDiagnosis(req.diagnosis());
            v.setNotes(req.notes());
            v.setConfidential(req.isConfidential());
            if (req.status() != null && !req.status().isBlank()) {
                try { v.setStatus(Visit.Status.valueOf(req.status().toUpperCase())); } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
            VisitDto dto = visitService.createForCurrent(patientId, v);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    // Aktualizacja wizyty (DTO)
    @PutMapping("/{id}")
    @Operation(summary = "Aktualizuj wizytę", description = "Aktualizuje wizytę na podstawie VisitUpdateRequest")
    public ResponseEntity<VisitDto> updateVisit(@PathVariable UUID id, @Valid @RequestBody VisitUpdateRequest req) {
        try {
            Visit updatedVisit = new Visit();
            updatedVisit.setVisitDate(req.visitDate());
            updatedVisit.setEndDate(req.endDate());
            updatedVisit.setVisitType(req.visitType());
            updatedVisit.setReason(req.reason());
            updatedVisit.setDiagnosis(req.diagnosis());
            updatedVisit.setNotes(req.notes());
            updatedVisit.setConfidential(req.isConfidential());
            if (req.status() != null && !req.status().isBlank()) {
                try { updatedVisit.setStatus(Visit.Status.valueOf(req.status().toUpperCase())); } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
            return visitService.updateForCurrent(id, updatedVisit)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    // Usuń wizytę
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVisit(@PathVariable UUID id) {
        try {
            boolean deleted = visitService.deleteForCurrent(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // Eksport ICS - Feed
    @GetMapping(value = "/ics-feed", produces = {"text/calendar", "application/ics", "application/octet-stream"})
    @Operation(summary = "Feed ICS", description = "Eksportuje kalendarz wizyt jako ICS z filtrami: doctorId, start, end, status")
    public ResponseEntity<byte[]> exportCalendarIcs(
            @RequestParam Optional<java.util.UUID> doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<java.time.Instant> start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<java.time.Instant> end,
            @RequestParam Optional<String> status
    ) {
        String ics = visitService.exportCalendarIcs(doctorId, start, end, status);
        byte[] data = ics.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "calendar"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=calendar.ics");
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
