package com.example.emr_server.repository;

import com.example.emr_server.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID>, JpaSpecificationExecutor<MedicalRecord> {

    /**
     * Pobiera listę rekordów medycznych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista rekordów medycznych powiązanych z pacjentem
     */
    List<MedicalRecord> findByPatient_Id(UUID patientId);

    /**
     * Pobiera listę rekordów medycznych utworzonych przez określonego użytkownika.
     *
     * @param createdById UUID użytkownika
     * @return lista rekordów medycznych utworzonych przez użytkownika
     */
    List<MedicalRecord> findByCreatedBy_Id(UUID createdById);

    /**
     * Pobiera listę rekordów medycznych o określonym typie.
     *
     * @param recordType typ rekordu (np. "Diagnosis", "Treatment Plan")
     * @return lista rekordów medycznych danego typu
     */
    List<MedicalRecord> findByRecordType(String recordType);

    /**
     * Pobiera listę rekordów medycznych utworzonych w podanym przedziale czasowym.
     *
     * @param startTimestamp początek okresu tworzenia
     * @param endTimestamp   koniec okresu tworzenia
     * @return lista rekordów medycznych utworzonych w podanym zakresie czasu
     */
    List<MedicalRecord> findByCreatedAtBetween(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera listę rekordów medycznych dla danego pacjenta i typu rekordu.
     *
     * @param patientId  UUID pacjenta
     * @param recordType typ rekordu
     * @return lista rekordów medycznych danego pacjenta i typu rekordu
     */
    List<MedicalRecord> findByPatient_IdAndRecordType(UUID patientId, String recordType);
}