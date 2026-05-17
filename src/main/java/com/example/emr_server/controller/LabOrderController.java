package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.LabOrderDto;
import com.example.emr_server.controller.dto.LabOrderItemDto;
import com.example.emr_server.controller.dto.request.LabOrderCreateRequest;
import com.example.emr_server.controller.dto.request.LabOrderItemUpdateRequest;
import com.example.emr_server.service.LabOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API zleceń laboratoryjnych (LabOrder + LabOrderItem).
 *
 * Autoryzacja:
 *  - utworzenie / anulowanie: doctor (z dostępem do pacjenta) lub admin
 *  - edycja wyniku pozycji: lab_tech, admin lub doctor (z dostępem)
 *  - odczyt: każdy zalogowany z dostępem do pacjenta
 */
@RestController
@RequestMapping("/api/lab-orders")
@Tag(name = "Zlecenia laboratoryjne", description = "Zlecenia badań i wprowadzanie wyników")
public class LabOrderController {

    private final LabOrderService labOrderService;

    public LabOrderController(LabOrderService labOrderService) {
        this.labOrderService = labOrderService;
    }

    @GetMapping
    @Operation(summary = "Lista zleceń", description = "Filtry: pacjent, status, priorytet, zakres czasu")
    public ResponseEntity<Page<LabOrderDto>> list(
            @RequestParam Optional<UUID> patientId,
            @RequestParam Optional<String> pesel,
            @RequestParam Optional<UUID> orderedById,
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> priority,
            @RequestParam Optional<Instant> from,
            @RequestParam Optional<Instant> to,
            Pageable pageable) {
        return ResponseEntity.ok(labOrderService.search(
                patientId, pesel, orderedById, status, priority, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegół zlecenia z pozycjami")
    public ResponseEntity<LabOrderDto> get(@PathVariable UUID id) {
        return labOrderService.getDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Utwórz zlecenie", description = "Z paneli i/lub pojedynczych testów")
    public ResponseEntity<LabOrderDto> create(@Valid @RequestBody LabOrderCreateRequest req) {
        LabOrderDto dto = labOrderService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Anuluj zlecenie")
    public ResponseEntity<LabOrderDto> cancel(@PathVariable UUID id,
                                              @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(labOrderService.cancel(id, reason));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('LAB_TECH','DOCTOR','ADMIN')")
    @Operation(summary = "Wprowadź / zaktualizuj wynik pozycji")
    public ResponseEntity<LabOrderItemDto> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody LabOrderItemUpdateRequest req) {
        return ResponseEntity.ok(labOrderService.updateItem(itemId, req));
    }
}

