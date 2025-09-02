package com.example.emr_server.service;

import com.example.emr_server.controller.dto.VisitSlotDto;
import com.example.emr_server.entity.VisitSlot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitSlotService {
    List<VisitSlotDto> list(Optional<UUID> doctorId, Optional<Instant> start, Optional<Instant> end, Optional<String> status);
    VisitSlotDto createForCurrent(VisitSlot slot);
    Optional<VisitSlotDto> reserve(UUID slotId, UUID patientId);
    Optional<VisitSlotDto> release(UUID slotId);
    boolean delete(UUID slotId);
}

