package com.example.emr_server.service;

import com.example.emr_server.entity.LabResult;
import com.example.emr_server.controller.dto.LabResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabResultService {

    /**
     * Pobiera listę wyników badań laboratoryjnych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista wyników powiązanych z pacjentem
     */
    List<LabResult> getLabResultsByPatientId(UUID patientId);

    /**
     * Pobiera listę wyników badań laboratoryjnych zleconych przez danego użytkownika.
     *
     * @param orderedById UUID użytkownika
     * @return lista wyników powiązanych z użytkownikiem zlecającym badanie
     */
    List<LabResult> getLabResultsByOrderedBy(UUID orderedById);

    /**
     * Pobiera listę wyników badań dla podanego typu testu.
     *
     * @param testName nazwa testu
     * @return lista wyników badań dla podanego typu testu
     */
    List<LabResult> getLabResultsByTestName(String testName);

    /**
     * Pobiera listę wyników badań wykonanych w podanym zakresie dat.
     *
     * @param startDate początkowa data wyników
     * @param endDate   końcowa data wyników
     * @return lista wyników badań wykonanych w podanym zakresie dat
     */
    List<LabResult> getLabResultsByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera listę wyników badań laboratoryjnych o danym statusie.
     *
     * @param status status badania
     * @return lista wyników badań o określonym statusie
     */
    List<LabResult> getLabResultsByStatus(String status);

    /**
     * Pobiera listę wyników badań zawierających fragment tekstu w wynikach.
     *
     * @param resultFragment fragment wyniku
     * @return lista badań zawierających określony fragment w wynikach
     */
    List<LabResult> getLabResultsByResultFragment(String resultFragment);

    /**
     * Zapisuje nowy wynik badań laboratoryjnych lub aktualizuje istniejący.
     *
     * @param labResult obiekt wyników badań laboratoryjnych do zapisania
     * @return zapisany obiekt wyników
     */
    LabResult saveLabResult(LabResult labResult);

    /**
     * Usuwa wynik badań laboratoryjnych na podstawie jego identyfikatora.
     *
     * @param labResultId UUID wyniku badań laboratoryjnych do usunięcia
     */
    void deleteLabResultById(UUID labResultId);

    /**
     * Pobiera listę widocznych wyników badań laboratoryjnych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista widocznych wyników powiązanych z pacjentem
     */
    List<LabResultDto> getVisibleByPatient(UUID patientId);

    /**
     * Pobiera listę widocznych wyników badań laboratoryjnych zleconych przez danego użytkownika.
     *
     * @param orderedById UUID użytkownika
     * @return lista widocznych wyników powiązanych z użytkownikiem zlecającym badanie
     */
    List<LabResultDto> getVisibleByOrderedBy(UUID orderedById);

    /**
     * Pobiera listę widocznych wyników badań laboratoryjnych o danym statusie.
     *
     * @param status status badania
     * @return lista widocznych wyników badań o określonym statusie
     */
    List<LabResultDto> getVisibleByStatus(String status);

    /**
     * Pobiera listę widocznych wyników badań laboratoryjnych wykonanych w podanym zakresie dat.
     *
     * @param startDate początkowa data wyników
     * @param endDate   końcowa data wyników
     * @return lista widocznych wyników badań wykonanych w podanym zakresie dat
     */
    List<LabResultDto> getVisibleByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera widoczny wynik badań laboratoryjnych na podstawie jego identyfikatora.
     *
     * @param id UUID wyniku badań laboratoryjnych
     * @return opcjonalnie zwracany widoczny wynik badań
     */
    Optional<LabResultDto> getVisibleById(UUID id);

    /**
     * Tworzy nowy wynik badań laboratoryjnych dla aktualnego użytkownika.
     *
     * @param labResult obiekt wyników badań laboratoryjnych do utworzenia
     * @return utworzony obiekt wyników
     */
    LabResultDto createForCurrent(LabResult labResult);

    /**
     * Aktualizuje istniejący wynik badań laboratoryjnych dla aktualnego użytkownika.
     *
     * @param id      UUID wyniku badań laboratoryjnych do aktualizacji
     * @param update  obiekt z danymi do aktualizacji
     * @return opcjonalnie zwracany zaktualizowany wynik badań
     */
    Optional<LabResultDto> updateForCurrent(UUID id, LabResult update);

    /**
     * Usuwa wynik badań laboratoryjnych dla aktualnego użytkownika.
     *
     * @param id UUID wyniku badań laboratoryjnych do usunięcia
     * @return true, jeśli usunięcie powiodło się, w przeciwnym razie false
     */
    boolean deleteForCurrent(UUID id);

    /**
     * Wyszukuje wyniki badań laboratoryjnych z filtrami i paginacją.
     *
     * @param patientId    opcjonalny UUID pacjenta
     * @param orderedById  opcjonalny UUID użytkownika zlecającego badanie
     * @param status        opcjonalny status badania
     * @param start        opcjonalna początkowa data wyników
     * @param end          opcjonalna końcowa data wyników
     * @param testName     opcjonalna nazwa testu
     * @param resultFragment opcjonalny fragment wyniku
     * @param pageable     obiekt Pageable zawierający informacje o paginacji i sortowaniu
     * @return strona wyników badań laboratoryjnych spełniających podane kryteria
     */
    Page<LabResultDto> searchVisible(
            Optional<UUID> patientId,
            Optional<UUID> orderedById,
            Optional<String> status,
            Optional<LocalDate> start,
            Optional<LocalDate> end,
            Optional<String> testName,
            Optional<String> resultFragment,
            Pageable pageable
    );
}