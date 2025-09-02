package com.example.emr_server.service;

import com.example.emr_server.entity.Prescription;
import com.example.emr_server.controller.dto.PrescriptionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionService {

    /**
     * Pobiera listę recept danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista recept powiązanych z pacjentem
     */
    List<Prescription> getPrescriptionsByPatientId(UUID patientId);

    /**
     * Pobiera listę recept wystawionych przez określonego lekarza.
     *
     * @param doctorId UUID lekarza
     * @return lista recept wystawionych przez danego lekarza
     */
    List<Prescription> getPrescriptionsByDoctorId(UUID doctorId);

    /**
     * Pobiera listę aktywnych recept, które jeszcze nie wygasły.
     *
     * @param currentDate data dzisiejsza
     * @return lista ważnych recept
     */
    List<Prescription> getActivePrescriptions(LocalDate currentDate);


    /**
     * Pobiera listę recept wystawionych w określonym przedziale dat.
     *
     * @param start początek zakresu
     * @param end   koniec zakresu
     * @return lista recept wystawionych w tym czasie
     */
    List<Prescription> getPrescriptionsByIssuedDateRange(LocalDate start, LocalDate end);

    /**
     * Zapisuje nową receptę lub aktualizuje istniejącą.
     *
     * @param prescription obiekt recepty do zapisania
     * @return zapisany obiekt recepty
     */
    Prescription savePrescription(Prescription prescription);

    /**
     * Usuwa receptę na podstawie jej identyfikatora.
     *
     * @param prescriptionId UUID recepty do usunięcia
     */
    void deletePrescriptionById(UUID prescriptionId);

    /**
     * Pobiera listę recept, które są widoczne dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista widocznych recept dla pacjenta
     */
    List<PrescriptionDto> getVisibleByPatient(UUID patientId);

    /**
     * Pobiera listę recept, które są widoczne dla danego lekarza.
     *
     * @param doctorId UUID lekarza
     * @return lista widocznych recept dla lekarza
     */
    List<PrescriptionDto> getVisibleByDoctor(UUID doctorId);

    /**
     * Pobiera listę aktywnych recept, które są widoczne dla danego użytkownika.
     *
     * @param now data dzisiejsza
     * @return lista aktywnych recept
     */
    List<PrescriptionDto> getVisibleActive(LocalDate now);

    /**
     * Pobiera listę recept na podstawie daty wystawienia w określonym przedziale czasowym,
     * które są widoczne dla danego użytkownika.
     *
     * @param start początek zakresu
     * @param end   koniec zakresu
     * @return lista widocznych recept w tym czasie
     */
    List<PrescriptionDto> getVisibleByIssuedDateRange(LocalDate start, LocalDate end);

    /**
     * Pobiera receptę na podstawie jej identyfikatora, jeśli jest widoczna dla danego użytkownika.
     *
     * @param id UUID recepty
     * @return opcjonalnie zwrócona widoczna recepta
     */
    Optional<PrescriptionDto> getVisibleById(UUID id);

    /**
     * Tworzy nową receptę dla aktualnego użytkownika na podstawie podanych danych.
     *
     * @param prescription dane recepty
     * @return utworzona recepta
     */
    PrescriptionDto createForCurrent(Prescription prescription);

    /**
     * Aktualizuje istniejącą receptę dla aktualnego użytkownika.
     *
     * @param id     UUID recepty do aktualizacji
     * @param update nowe dane recepty
     * @return opcjonalnie zwrócona zaktualizowana recepta
     */
    Optional<PrescriptionDto> updateForCurrent(UUID id, Prescription update);

    /**
     * Usuwa receptę dla aktualnego użytkownika.
     *
     * @param id UUID recepty do usunięcia
     * @return true, jeśli usunięcie się powiodło, w przeciwnym razie false
     */
    boolean deleteForCurrent(UUID id);

    /**
     * Wyszukuje recepty według filtrów z paginacją i sortowaniem.
     *
     * @param patientId UUID pacjenta (opcjonalnie)
     * @param doctorId UUID lekarza (opcjonalnie)
     * @param active flaga aktywności recepty (opcjonalnie)
     * @param start początek zakresu daty wystawienia (opcjonalnie)
     * @param end koniec zakresu daty wystawienia (opcjonalnie)
     * @param pageable informacje o paginacji i sortowaniu
     * @return strona z listą dopasowanych recept
     */
    Page<PrescriptionDto> searchVisible(
            Optional<UUID> patientId,
            Optional<UUID> doctorId,
            Optional<Boolean> active,
            Optional<LocalDate> start,
            Optional<LocalDate> end,
            Pageable pageable
    );
}