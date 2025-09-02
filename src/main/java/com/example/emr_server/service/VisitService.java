package com.example.emr_server.service;

import com.example.emr_server.entity.Visit;
import com.example.emr_server.controller.dto.VisitDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VisitService {
    Visit getVisitById(UUID id);
    List<Visit> getVisitsByPatientId(UUID patientId);
    List<Visit> getVisitsByDoctorId(UUID doctorId);
    List<Visit> getVisitsByDateRange(Instant start, Instant end);
    Visit saveVisit(Visit visit);
    void deleteVisit(UUID id);

    List<Visit> findAll();

    Optional<Visit> findById(UUID id);

    Visit save(Visit visit);

    boolean existsById(UUID id);

    void deleteById(UUID id);

    Optional<Visit> updateVisit(UUID id, Visit update);

    // Nowe metody DTO + autoryzacja
    List<VisitDto> getAllVisibleForCurrent();
    Optional<VisitDto> getVisibleById(UUID id);
    VisitDto createForCurrent(UUID patientId, Visit visit);
    Optional<VisitDto> updateForCurrent(UUID id, Visit update);
    boolean deleteForCurrent(UUID id);

    // Nowe: wyszukiwanie z filtrami (AND) + paginacja/sort
    Page<VisitDto> searchVisible(
            Optional<UUID> patientId,
            Optional<UUID> doctorId,
            Optional<Instant> start,
            Optional<Instant> end,
            Optional<String> type,
            Optional<String> diagnosis,
            Optional<String> reason,
            Optional<Boolean> confidential,
            Optional<String> status,
            Pageable pageable
    );

    // Eksport ICS dla wizyty
    Optional<String> exportIcs(UUID visitId);

    // Eksport ICS dla zakresu wizyt (kalendarz)
    String exportCalendarIcs(
            Optional<UUID> doctorId,
            Optional<Instant> start,
            Optional<Instant> end,
            Optional<String> status
    );
}