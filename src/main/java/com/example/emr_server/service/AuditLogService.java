package com.example.emr_server.service;

import com.example.emr_server.entity.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogService {

    /**
     * Pobiera listę zapisów dziennika audytu dla danego użytkownika.
     *
     * @param userId ID użytkownika
     * @return lista zapisów związanych z użytkownikiem
     */
    List<AuditLog> getLogsByUserId(UUID userId);

    /**
     * Pobiera listę zapisów dziennika audytu dla danego pacjenta.
     *
     * @param patientId ID pacjenta
     * @return lista zapisów związanych z pacjentem
     */
    List<AuditLog> getLogsByPatientId(UUID patientId);

    /**
     * Pobiera listę zapisów dziennika audytu dla podanego typu akcji.
     *
     * @param action Typ akcji (np. "CREATE", "UPDATE")
     * @return lista zapisów dla danego typu akcji
     */
    List<AuditLog> getLogsByAction(String action);

    /**
     * Pobiera listę zapisów dziennika audytu wykonanych w określonym zakresie czasowym.
     *
     * @param startTimestamp Początkowy znacznik czasu
     * @param endTimestamp   Końcowy znacznik czasu
     * @return lista zapisów z zakresu czasowego
     */
    List<AuditLog> getLogsByTimestampRange(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera listę zapisów dziennika audytu zawierających dany fragment w opisie.
     *
     * @param descriptionFragment Fragment opisu
     * @return lista zapisów zawierających fragment w opisie
     */
    List<AuditLog> getLogsByDescriptionFragment(String descriptionFragment);

    /**
     * Zapisuje nowy wpis do dziennika audytu.
     *
     * @param auditLog Obiekt dziennika audytu do zapisania
     * @return zapisany obiekt dziennika audytu
     */
    AuditLog saveLog(AuditLog auditLog);

    /**
     * Usuwa wpis z dziennika audytu na podstawie jego ID.
     *
     * @param logId ID wpisu do usunięcia
     */
    void deleteLogById(Integer logId);
}