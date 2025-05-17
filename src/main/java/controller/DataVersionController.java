package controller;

import entity.DataVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.DataVersionService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/data-versions")
public class DataVersionController {

    @Autowired
    private DataVersionService dataVersionService;

    /**
     * Pobierz wersje danych dla encji i zakresu czasu.
     */
    @GetMapping
    public List<DataVersion> getVersionsWithDetails(@RequestParam String entityType,
                                                    @RequestParam UUID entityId,
                                                    @RequestParam Instant startTime,
                                                    @RequestParam Instant endTime) {
        return dataVersionService.getDataVersionsByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Usuń wersję danych.
     */
    @DeleteMapping("/{versionId}")
    public ResponseEntity<?> deleteVersion(@PathVariable UUID versionId) {
        dataVersionService.deleteDataVersionById(versionId);
        return ResponseEntity.noContent().build();
    }
}