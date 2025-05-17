package service;

import entity.MedicalRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MedicalRecordService {

    /**
     * Pobiera listę rekordów medycznych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista rekordów medycznych powiązanych z pacjentem
     */
    List<MedicalRecord> getMedicalRecordsByPatientId(UUID patientId);

    /**
     * Pobiera listę rekordów medycznych utworzonych przez określonego użytkownika.
     *
     * @param createdById UUID użytkownika
     * @return lista rekordów medycznych utworzonych przez użytkownika
     */
    List<MedicalRecord> getMedicalRecordsByCreatedBy(UUID createdById);

    /**
     * Pobiera listę rekordów medycznych o określonym typie.
     *
     * @param recordType typ rekordu (np. "Diagnosis", "Treatment Plan")
     * @return lista rekordów medycznych danego typu
     */
    List<MedicalRecord> getMedicalRecordsByType(String recordType);

    /**
     * Pobiera listę rekordów medycznych utworzonych w podanym przedziale czasowym.
     *
     * @param startTimestamp początek okresu tworzenia
     * @param endTimestamp   koniec okresu tworzenia
     * @return lista rekordów medycznych utworzonych w podanym zakresie czasu
     */
    List<MedicalRecord> getMedicalRecordsByCreationRange(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera listę rekordów medycznych dla danego pacjenta i typu rekordu.
     *
     * @param patientId  UUID pacjenta
     * @param recordType typ rekordu
     * @return lista rekordów medycznych danego pacjenta i typu rekordu
     */
    List<MedicalRecord> getMedicalRecordsByPatientIdAndType(UUID patientId, String recordType);

    /**
     * Zapisuje nowy rekord medyczny lub aktualizuje istniejący.
     *
     * @param medicalRecord obiekt rekordu medycznego do zapisania
     * @return zapisany obiekt rekordu medycznego
     */
    MedicalRecord saveMedicalRecord(MedicalRecord medicalRecord);

    /**
     * Usuwa rekord medyczny na podstawie jego identyfikatora.
     *
     * @param medicalRecordId UUID rekordu medycznego do usunięcia
     */
    void deleteMedicalRecordById(UUID medicalRecordId);
}