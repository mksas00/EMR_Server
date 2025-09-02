package com.example.emr_server.repository;

import com.example.emr_server.entity.MedicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationHistoryRepository extends JpaRepository<MedicationHistory, UUID> {

    /**
     * Pobiera historię leków dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista historii leków powiązana z pacjentem
     */
    List<MedicationHistory> findByPatient_Id(UUID patientId);

    /**
     * Pobiera historię leków aktywną w określonym dniu (aktualnie zażywane lub mające wpływ w tym dniu).
     *
     * @param date data, dla której weryfikujemy aktywność leków
     * @return lista aktywnych leków w podanym dniu
     */
    List<MedicationHistory> findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrEndDateIsNull(LocalDate date, LocalDate date2);

    /**
     * Pobiera historię leków dla danego pacjenta w podanym zakresie dat.
     *
     * @param patientId UUID pacjenta
     * @param startDate data początkowa
     * @param endDate   data końcowa
     * @return lista wszystkich leków w tym czasie dla konkretnego pacjenta
     */
    List<MedicationHistory> findByPatient_IdAndStartDateBetween(UUID patientId, LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera listę leków z podanym powodem zastosowania.
     *
     * @param reason powód rozpoczęcia stosowania leku
     * @return lista leków związanych z określonym powodem
     */
    List<MedicationHistory> findByReasonContaining(String reason);

    /**
     * Pobiera listę leków o określonej nazwie dla danego pacjenta.
     *
     * @param patientId  UUID pacjenta
     * @param medicationId  UUID leku
     * @return lista historii leków o podanej nazwie
     */
    List<MedicationHistory> findByPatient_IdAndMedicationRef_Id(UUID patientId, UUID medicationId);
}