package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.VisitSlotDto;
import com.example.emr_server.controller.dto.request.VisitSlotCreateRequest;
import com.example.emr_server.entity.User;
import com.example.emr_server.entity.VisitSlot;
import com.example.emr_server.service.VisitSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/sloty-wizyt")
@Tag(name = "Sloty wizyt", description = "Zarządzanie wolnymi terminami wizyt")
public class VisitSlotController {

    private final VisitSlotService visitSlotService;

    public VisitSlotController(VisitSlotService visitSlotService) {
        this.visitSlotService = visitSlotService;
    }

    @GetMapping
    @Operation(summary = "Lista slotów", description = "Filtrowanie po lekarzu, przedziale czasu i statusie")
    public ResponseEntity<List<VisitSlotDto>> list(@RequestParam Optional<UUID> doctorId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> start,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> end,
                                                   @RequestParam Optional<String> status) {
        List<VisitSlotDto> out = visitSlotService.list(doctorId, start, end, status);
        return ResponseEntity.ok(out);
    }

    @PostMapping
    @Operation(summary = "Utwórz slot", description = "Tworzy slot dla bieżącego lekarza lub wskazanego (admin)")
    public ResponseEntity<VisitSlotDto> create(@Valid @RequestBody VisitSlotCreateRequest req) {
        try {
            VisitSlot s = new VisitSlot();
            if (req.doctorId() != null) { var d = new User(); d.setId(req.doctorId()); s.setDoctor(d); }
            s.setStartTime(req.startTime());
            s.setEndTime(req.endTime());
            if (req.status() != null && !req.status().isBlank()) {
                try { s.setStatus(VisitSlot.Status.valueOf(req.status().toUpperCase())); } catch (IllegalArgumentException ignored) {}
            }
            VisitSlotDto dto = visitSlotService.createForCurrent(s);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @PostMapping("/{id}/reserve")
    @Operation(summary = "Rezerwuj slot", description = "Rezerwuje slot dla pacjenta i tworzy wizytę")
    public ResponseEntity<VisitSlotDto> reserve(@PathVariable UUID id, @RequestParam UUID patientId) {
        try {
            return visitSlotService.reserve(id, patientId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Zwolnij slot", description = "Anuluje powiązaną wizytę i zwalnia slot")
    public ResponseEntity<VisitSlotDto> release(@PathVariable UUID id) {
        try {
            return visitSlotService.release(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usuń slot", description = "Usuwa slot jeśli nie jest zarezerwowany")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        try {
            boolean deleted = visitSlotService.delete(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
