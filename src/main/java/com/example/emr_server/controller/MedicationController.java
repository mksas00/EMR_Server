package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.MedicationDto;
import com.example.emr_server.controller.dto.MedicationPackageDto;
import com.example.emr_server.service.MedicationService;
import com.example.emr_server.service.dto.MedicationUrplImportRequest;
import com.example.emr_server.service.urpl.UrplImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/leki")
@Tag(name = "Leki", description = "Wyszukiwanie leków i opakowań oraz import URPL")
public class MedicationController {

    private final MedicationService medicationService;
    private final UrplImportService urplImportService;

    public MedicationController(MedicationService medicationService, UrplImportService urplImportService) {
        this.medicationService = medicationService;
        this.urplImportService = urplImportService;
    }

    @GetMapping
    @Operation(summary = "Szukaj leków", description = "Wyszukiwanie po nazwie lub ATC")
    public ResponseEntity<Page<MedicationDto>> search(@RequestParam Optional<String> q,
                                                      @RequestParam Optional<String> atc,
                                                      Pageable pageable) {
        Page<MedicationDto> page = medicationService.search(q.orElse(null), atc.orElse(null), pageable);
        return ResponseEntity.ok(page);

    }

    @GetMapping("/opakowania")
    @Operation(summary = "Szukaj opakowania po GTIN", description = "Zwraca opakowanie na podstawie numeru GTIN")
    public ResponseEntity<MedicationPackageDto> findByGtin(@RequestParam String gtin) {
        return medicationService.findPackageByGtin(gtin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/import-urpl")
    @Operation(summary = "Import rekordu URPL", description = "Upsert leku i opakowania na podstawie danych z URPL")
    public ResponseEntity<MedicationDto> importUrpl(@Valid @RequestBody MedicationUrplImportRequest req) {
        MedicationDto dto = medicationService.importUrplRecord(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/import-urpl/bulk")
    @Operation(summary = "Import URPL – wsadowy", description = "Stronicowane pobieranie z URPL i zapis do bazy")
    public ResponseEntity<String> importUrplBulk(@RequestParam(defaultValue = "0") int startPage,
                                                 @RequestParam(defaultValue = "100") int size,
                                                 @RequestParam(defaultValue = "5") int maxPages) {
        int imported = urplImportService.runImport(startPage, size, maxPages);
        return ResponseEntity.ok("imported=" + imported);
    }
}

