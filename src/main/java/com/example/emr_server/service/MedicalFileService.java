package com.example.emr_server.service;

import com.example.emr_server.entity.MedicalFile;
import com.example.emr_server.controller.dto.MedicalFileDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalFileService {

    /**
     * Pobiera listę plików medycznych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista plików medycznych powiązanych z pacjentem
     */
    List<MedicalFile> getMedicalFilesByPatientId(UUID patientId);

    /**
     * Pobiera listę plików medycznych przesłanych przez określonego użytkownika.
     *
     * @param uploadedById UUID użytkownika
     * @return lista plików medycznych przesłanych przez użytkownika
     */
    List<MedicalFile> getMedicalFilesByUploadedBy(UUID uploadedById);

    /**
     * Pobiera listę plików medycznych o określonym typie MIME.
     *
     * @param mimeType typ MIME
     * @return lista plików o podanym typie MIME
     */
    List<MedicalFile> getMedicalFilesByMimeType(String mimeType);

    /**
     * Pobiera listę plików medycznych przesłanych w podanym zakresie czasowym.
     *
     * @param startTimestamp znacznik początkowego czasu
     * @param endTimestamp   znacznik końcowego czasu
     * @return lista plików przesłanych w danym zakresie czasowym
     */
    List<MedicalFile> getMedicalFilesByUploadedAtRange(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera listę plików medycznych dla danego pacjenta i typu MIME.
     *
     * @param patientId UUID pacjenta
     * @param mimeType  typ MIME
     * @return lista plików powiązanych z pacjentem i typem MIME
     */
    List<MedicalFile> getMedicalFilesByPatientIdAndMimeType(UUID patientId, String mimeType);

    /**
     * Pobiera listę plików na podstawie fragmentu nazwy pliku.
     *
     * @param fileNameFragment fragment nazwy pliku
     * @return lista plików, których nazwa zawiera podany fragment
     */
    List<MedicalFile> getMedicalFilesByFileNameFragment(String fileNameFragment);

    /**
     * Zapisuje nowy plik medyczny lub aktualizuje istniejący.
     *
     * @param medicalFile obiekt pliku medycznego do zapisania
     * @return zapisany obiekt pliku medycznego
     */
    MedicalFile saveMedicalFile(MedicalFile medicalFile);

    /**
     * Usuwa plik medyczny na podstawie jego identyfikatora.
     *
     * @param medicalFileId UUID pliku medycznego do usunięcia
     */
    void deleteMedicalFileById(UUID medicalFileId);

    /**
     * Pobiera listę plików medycznych widocznych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista widocznych plików medycznych powiązanych z pacjentem
     */
    List<MedicalFileDto> getVisibleByPatient(UUID patientId);

    /**
     * Pobiera listę plików medycznych widocznych przesłanych przez określonego użytkownika.
     *
     * @param uploadedById UUID użytkownika
     * @return lista widocznych plików medycznych przesłanych przez użytkownika
     */
    List<MedicalFileDto> getVisibleByUploadedBy(UUID uploadedById);

    /**
     * Pobiera listę plików medycznych widocznych o określonym typie MIME.
     *
     * @param mimeType typ MIME
     * @return lista widocznych plików o podanym typie MIME
     */
    List<MedicalFileDto> getVisibleByMimeType(String mimeType);

    /**
     * Pobiera listę plików medycznych widocznych przesłanych w podanym zakresie czasowym.
     *
     * @param startTimestamp znacznik początkowego czasu
     * @param endTimestamp   znacznik końcowego czasu
     * @return lista widocznych plików przesłanych w danym zakresie czasowym
     */
    List<MedicalFileDto> getVisibleByUploadedAtRange(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera widoczny plik medyczny na podstawie jego identyfikatora.
     *
     * @param id UUID pliku medycznego
     * @return opcjonalnie widoczny plik medyczny
     */
    Optional<MedicalFileDto> getVisibleById(UUID id);

    /**
     * Tworzy nowy plik medyczny dla bieżącego użytkownika.
     *
     * @param file obiekt pliku medycznego do utworzenia
     * @return utworzony obiekt pliku medycznego
     */
    MedicalFileDto createForCurrent(MedicalFile file);

    /**
     * Aktualizuje istniejący plik medyczny dla bieżącego użytkownika.
     *
     * @param id     UUID pliku medycznego do aktualizacji
     * @param update obiekt z danymi do aktualizacji
     * @return opcjonalnie zaktualizowany plik medyczny
     */
    Optional<MedicalFileDto> updateForCurrent(UUID id, MedicalFile update);

    /**
     * Usuwa plik medyczny dla bieżącego użytkownika.
     *
     * @param id UUID pliku medycznego do usunięcia
     * @return true, jeśli usunięcie powiodło się, w przeciwnym razie false
     */
    boolean deleteForCurrent(UUID id);

    /**
     * Wyszukuje widoczne pliki medyczne na podstawie podanych filtrów i paginuje wyniki.
     *
     * @param patientId         opcjonalny UUID pacjenta
     * @param uploadedById      opcjonalny UUID użytkownika, który przesłał plik
     * @param mimeType          opcjonalny typ MIME pliku
     * @param start              opcjonalny znacznik początkowego czasu przesłania
     * @param end                opcjonalny znacznik końcowego czasu przesłania
     * @param fileNameFragment   opcjonalny fragment nazwy pliku
     * @param pageable           obiekt Pageable zawierający informacje o stronie i rozmiarze strony
     * @return strona z listą widocznych plików medycznych spełniających podane kryteria
     */
    Page<MedicalFileDto> searchVisible(
            Optional<UUID> patientId,
            Optional<UUID> uploadedById,
            Optional<String> mimeType,
            Optional<Instant> start,
            Optional<Instant> end,
            Optional<String> fileNameFragment,
            Pageable pageable
    );
}