package service;

import entity.LabResult;

import java.time.LocalDate;
import java.util.List;
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
}