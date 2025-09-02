package com.example.emr_server.service;

import com.example.emr_server.entity.MedicationHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MedicationHistoryService {

    /**
     * Pobiera historię leków dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista historii leków powiązana z pacjentem
     */
    List<MedicationHistory> getMedicationHistoryByPatientId(UUID patientId);

    /**
     * Pobiera historię leków aktywną w określonym dniu.
     *
     * @param date data, dla której weryfikujemy aktywność leków
     * @return lista aktywnych leków w podanym dniu
     */
    List<MedicationHistory> getActiveMedicationsOnDate(LocalDate date);

    /**
     * Pobiera historię leków dla danego pacjenta w podanym zakresie dat.
     *
     * @param patientId UUID pacjenta
     * @param startDate data początkowa
     * @param endDate   data końcowa
     * @return lista wszystkich leków w tym czasie dla konkretnego pacjenta
     */
    List<MedicationHistory> getMedicationHistoryByPatientIdAndDateRange(UUID patientId, LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera historię leków z podanym powodem zastosowania.
     *
     * @param reason powód rozpoczęcia stosowania leku
     * @return lista leków związanych z określonym powodem
     */
    List<MedicationHistory> getMedicationHistoryByReason(String reason);

    /**
     * Pobiera listę leków o określonym medicationId dla danego pacjenta.
     *
     * @param patientId  UUID pacjenta
     * @param medicationId UUID leku
     * @return lista historii leków o podanym medicationId
     */
    List<MedicationHistory> getMedicationHistoryByPatientIdAndMedicationId(UUID patientId, UUID medicationId);

    /**
     * Zapisuje historię leku.
     *
     * @param medicationHistory obiekt historii leku do zapisania
     * @return zapisany obiekt historii leku
     */
    MedicationHistory saveMedicationHistory(MedicationHistory medicationHistory);

    /**
     * Usuwa historię leku na podstawie jej identyfikatora.
     *
     * @param medicationHistoryId UUID historii leku do usunięcia
     */
    void deleteMedicationHistoryById(UUID medicationHistoryId);
}