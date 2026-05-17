package com.example.emr_server.service;

import com.example.emr_server.controller.dto.LabOrderDto;
import com.example.emr_server.controller.dto.LabOrderItemDto;
import com.example.emr_server.controller.dto.LabPanelDto;
import com.example.emr_server.controller.dto.request.LabOrderCreateRequest;
import com.example.emr_server.controller.dto.request.LabOrderItemUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabOrderService {

    /** Lista zleceń z filtrami (autoryzacja stosowana per pacjent). */
    Page<LabOrderDto> search(
            Optional<UUID> patientId,
            Optional<String> pesel,
            Optional<UUID> orderedById,
            Optional<String> status,
            Optional<String> priority,
            Optional<Instant> from,
            Optional<Instant> to,
            Pageable pageable);

    /** Szczegół zlecenia z pozycjami (lub empty gdy brak dostępu / brak zlecenia). */
    Optional<LabOrderDto> getDetail(UUID orderId);

    /** Tworzy zlecenie z pozycjami z paneli i/lub ręcznych. */
    LabOrderDto create(LabOrderCreateRequest req);

    /** Anuluje całe zlecenie (i wszystkie nieukończone pozycje). */
    LabOrderDto cancel(UUID orderId, String reason);

    /** Wprowadza/aktualizuje wynik pojedynczej pozycji (lab_tech / doctor). */
    LabOrderItemDto updateItem(UUID itemId, LabOrderItemUpdateRequest req);

    /** Lista paneli (katalog). */
    List<LabPanelDto> listActivePanels();
}

