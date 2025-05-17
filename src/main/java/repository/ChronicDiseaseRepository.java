package repository;

import entity.ChronicDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChronicDiseaseRepository extends JpaRepository<ChronicDisease, UUID> {

    /**
     * Pobiera listę chorób przewlekłych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista chorób przewlekłych powiązanych z pacjentem
     */
    List<ChronicDisease> findByPatient_Id(UUID patientId);

    /**
     * Pobiera listę chorób przewlekłych o podanej nazwie.
     *
     * @param diseaseName nazwa choroby
     * @return lista chorób przewlekłych związanych z konkretną nazwą choroby
     */
    List<ChronicDisease> findByDiseaseName(String diseaseName);

    /**
     * Pobiera listę chorób przewlekłych zdiagnozowanych w określonym przedziale czasowym.
     *
     * @param startDate data początkowa
     * @param endDate   data końcowa
     * @return lista chorób przewlekłych zdiagnozowanych w podanym zakresie dat
     */
    List<ChronicDisease> findByDiagnosedDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera listę chorób przewlekłych, których notatki zawierają podany tekst.
     *
     * @param notesFragment fragment notatek
     * @return lista chorób przewlekłych, których notatki zawierają podany tekst
     */
    List<ChronicDisease> findByNotesContaining(String notesFragment);
}