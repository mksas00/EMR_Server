package service;

import entity.ChronicDisease;

import java.time.LocalDate;
import java.util.List;
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
}