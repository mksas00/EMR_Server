package com.example.emr_server.service;

import com.example.emr_server.entity.ChronicDisease;
import com.example.emr_server.controller.dto.ChronicDiseaseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChronicDiseaseService {

    /**
     * Pobiera listę chorób przewlekłych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista chorób przewlekłych powiązanych z pacjentem
     */
    List<ChronicDisease> getDiseasesByPatientId(UUID patientId);

    /**
     * Pobiera listę chorób przewlekłych o podanej nazwie.
     *
     * @param diseaseName nazwa choroby
     * @return lista chorób przewlekłych związanych z nazwą choroby
     */
    List<ChronicDisease> getDiseasesByName(String diseaseName);

    /**
     * Pobiera listę chorób przewlekłych zdiagnozowanych w danym zakresie dat.
     *
     * @param startDate data początkowa
     * @param endDate   data końcowa
     * @return lista chorób przewlekłych w podanym zakresie dat
     */
    List<ChronicDisease> getDiseasesByDiagnosedDates(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera listę chorób przewlekłych, których notatki zawierają podany tekst.
     *
     * @param notesFragment fragment notatek
     * @return lista chorób przewlekłych zawierających dane słowa kluczowe w notatkach
     */
    List<ChronicDisease> getDiseasesByNotesFragment(String notesFragment);

    /**
     * Zapisuje nową lub aktualizuje istniejącą chorobę przewlekłą.
     *
     * @param disease obiekt choroby przewlekłej do zapisania
     * @return zapisany obiekt choroby przewlekłej
     */
    ChronicDisease saveDisease(ChronicDisease disease);

    /**
     * Usuwa daną chorobę przewlekłą na podstawie jej identyfikatora.
     *
     * @param diseaseId UUID choroby przewlekłej do usunięcia
     */
    void deleteDiseaseById(UUID diseaseId);

    /**
     * Pobiera widoczne choroby przewlekłe dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista widocznych chorób przewlekłych powiązanych z pacjentem
     */
    List<ChronicDiseaseDto> getVisibleByPatient(UUID patientId);

    /**
     * Pobiera widoczne choroby przewlekłe o podanej nazwie.
     *
     * @param diseaseName nazwa choroby
     * @return lista widocznych chorób przewlekłych związanych z nazwą choroby
     */
    List<ChronicDiseaseDto> getVisibleByName(String diseaseName);

    /**
     * Pobiera widoczne choroby przewlekłe zdiagnozowane w danym zakresie dat.
     *
     * @param startDate data początkowa
     * @param endDate   data końcowa
     * @return lista widocznych chorób przewlekłych w podanym zakresie dat
     */
    List<ChronicDiseaseDto> getVisibleByDiagnosedDates(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera widoczną chorobę przewlekłą na podstawie jej identyfikatora.
     *
     * @param id UUID choroby przewlekłej
     * @return opcjonalnie zwracana widoczna choroba przewlekła
     */
    Optional<ChronicDiseaseDto> getVisibleById(UUID id);

    /**
     * Tworzy nową chorobę przewlekłą dla aktualnego użytkownika.
     *
     * @param disease obiekt choroby przewlekłej do utworzenia
     * @return utworzony obiekt choroby przewlekłej
     */
    ChronicDiseaseDto createForCurrent(ChronicDisease disease);

    /**
     * Aktualizuje istniejącą chorobę przewlekłą dla aktualnego użytkownika.
     *
     * @param id      UUID choroby przewlekłej do aktualizacji
     * @param update  obiekt z danymi do aktualizacji
     * @return opcjonalnie zwracana zaktualizowana choroba przewlekła
     */
    Optional<ChronicDiseaseDto> updateForCurrent(UUID id, ChronicDisease update);

    /**
     * Usuwa chorobę przewlekłą dla aktualnego użytkownika.
     *
     * @param id UUID choroby przewlekłej do usunięcia
     * @return true, jeśli usunięcie powiodło się, w przeciwnym razie false
     */
    boolean deleteForCurrent(UUID id);

    /**
     * Wyszukuje widoczne choroby przewlekłe z zastosowaniem filtrów i paginacji.
     *
     * @param patientId     opcjonalny UUID pacjenta
     * @param diseaseName   opcjonalna nazwa choroby
     * @param start         opcjonalna data początkowa diagnozy
     * @param end           opcjonalna data końcowa diagnozy
     * @param notesFragment opcjonalny fragment notatek do wyszukania
     * @param pageable      obiekt zawierający informacje o stronie i rozmiarze strony
     * @return strona zawierająca pasujące choroby przewlekłe
     */
    Page<ChronicDiseaseDto> searchVisible(
            Optional<UUID> patientId,
            Optional<String> diseaseName,
            Optional<LocalDate> start,
            Optional<LocalDate> end,
            Optional<String> notesFragment,
            Pageable pageable
    );
}