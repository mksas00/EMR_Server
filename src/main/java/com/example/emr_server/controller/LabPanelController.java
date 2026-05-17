package com.example.emr_server.controller;

import com.example.emr_server.controller.dto.LabPanelDto;
import com.example.emr_server.service.LabOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Katalog predefiniowanych paneli laboratoryjnych. */
@RestController
@RequestMapping("/api/lab-panels")
@Tag(name = "Panele laboratoryjne", description = "Predefiniowane zestawy badań")
public class LabPanelController {

    private final LabOrderService labOrderService;

    public LabPanelController(LabOrderService labOrderService) {
        this.labOrderService = labOrderService;
    }

    @GetMapping
    @Operation(summary = "Lista aktywnych paneli")
    public ResponseEntity<List<LabPanelDto>> list() {
        return ResponseEntity.ok(labOrderService.listActivePanels());
    }
}

