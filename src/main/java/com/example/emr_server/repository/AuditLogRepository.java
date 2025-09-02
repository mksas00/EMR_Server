package com.example.emr_server.repository;

import com.example.emr_server.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    /**
     * Pobiera listę zapisów dziennika audytu dla danego użytkownika.
     *
     * @param userId ID użytkownika
     * @return lista zapisów związanych z użytkownikiem
     */
    List<AuditLog> findByUser_Id(UUID userId);

    /**
     * Pobiera listę zapisów dziennika audytu dla danego pacjenta.
     *
     * @param patientId ID pacjenta
     * @return lista zapisów związanych z pacjentem
     */
    List<AuditLog> findByPatient_Id(UUID patientId);

    /**
     * Pobiera listę zapisów dziennika audytu dla podanej akcji.
     *
     * @param action Typ akcji (np. "CREATE", "UPDATE")
     * @return lista zapisów dla danego typu akcji
     */
    List<AuditLog> findByAction(String action);

    /**
     * Pobiera listę zapisów dziennika audytu wykonanych w określonym zakresie czasowym.
     *
     * @param startTimestamp Początkowy znacznik czasu
     * @param endTimestamp   Końcowy znacznik czasu
     * @return lista zapisów z zakresu czasowego
     */
    List<AuditLog> findByTimestampBetween(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera listę zapisów dziennika audytu zawierających podany fragment w opisie.
     *
     * @param descriptionFragment Fragment opisu
     * @return lista zapisów, które zawierają fragment w opisie
     */
    List<AuditLog> findByDescriptionContaining(String descriptionFragment);
}