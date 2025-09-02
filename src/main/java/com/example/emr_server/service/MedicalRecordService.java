package com.example.emr_server.service;

import com.example.emr_server.entity.MedicalRecord;
import com.example.emr_server.controller.dto.MedicalRecordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    /**
     * Pobiera listę rekordów medycznych widocznych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista widocznych rekordów medycznych dla pacjenta
     */
    List<MedicalRecordDto> getVisibleByPatient(UUID patientId);

    /**
     * Pobiera listę rekordów medycznych widocznych dla danego użytkownika.
     *
     * @param createdById UUID użytkownika
     * @return lista widocznych rekordów medycznych dla użytkownika
     */
    List<MedicalRecordDto> getVisibleByCreatedBy(UUID createdById);

    /**
     * Pobiera listę rekordów medycznych danego typu, które są widoczne dla użytkownika.
     *
     * @param recordType typ rekordu (np. "Diagnosis", "Treatment Plan")
     * @return lista widocznych rekordów medycznych danego typu
     */
    List<MedicalRecordDto> getVisibleByType(String recordType);

    /**
     * Pobiera listę rekordów medycznych utworzonych w podanym przedziale czasowym, które są widoczne dla użytkownika.
     *
     * @param startTimestamp początek okresu tworzenia
     * @param endTimestamp   koniec okresu tworzenia
     * @return lista widocznych rekordów medycznych utworzonych w podanym zakresie czasu
     */
    List<MedicalRecordDto> getVisibleByCreationRange(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera rekord medyczny o podanym identyfikatorze, jeśli jest widoczny dla użytkownika.
     *
     * @param id UUID rekordu medycznego
     * @return opcjonalnie rekord medyczny, jeśli istnieje i jest widoczny
     */
    Optional<MedicalRecordDto> getVisibleById(UUID id);

    /**
     * Tworzy nowy rekord medyczny dla aktualnego użytkownika.
     *
     * @param medicalRecord obiekt rekordu medycznego do utworzenia
     * @return utworzony obiekt rekordu medycznego
     */
    MedicalRecordDto createForCurrent(MedicalRecord medicalRecord);

    /**
     * Aktualizuje istniejący rekord medyczny dla aktualnego użytkownika.
     *
     * @param id      UUID rekordu medycznego do aktualizacji
     * @param update  obiekt z danymi do aktualizacji
     * @return opcjonalnie zaktualizowany rekord medyczny, jeśli istnieje i należy do aktualnego użytkownika
     */
    Optional<MedicalRecordDto> updateForCurrent(UUID id, MedicalRecord update);

    /**
     * Usuwa rekord medyczny dla aktualnego użytkownika.
     *
     * @param id UUID rekordu medycznego do usunięcia
     * @return true, jeśli rekord został usunięty, w przeciwnym razie false
     */
    boolean deleteForCurrent(UUID id);

    /**
     * Wyszukuje widoczne rekordy medyczne według podanych filtrów z paginacją.
     *
     * @param patientId  opcjonalnie UUID pacjenta
     * @param createdById opcjonalnie UUID użytkownika, który utworzył rekord
     * @param type        opcjonalnie typ rekordu (np. "Diagnosis", "Treatment Plan")
     * @param start       opcjonalnie początek okresu tworzenia
     * @param end         opcjonalnie koniec okresu tworzenia
     * @param pageable     obiekt zawierający informacje o stronie i rozmiarze strony
     * @return strona widocznych rekordów medycznych pasujących do filtrów
     */
    Page<MedicalRecordDto> searchVisible(
            Optional<UUID> patientId,
            Optional<UUID> createdById,
            Optional<String> type,
            Optional<Instant> start,
            Optional<Instant> end,
            Pageable pageable
    );
}