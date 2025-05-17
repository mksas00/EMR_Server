package repository;

import entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, UUID> {

    /**
     * Pobiera listę wyników badań laboratoryjnych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista wyników powiązanych z pacjentem
     */
    List<LabResult> findByPatient_Id(UUID patientId);

    /**
     * Pobiera listę wyników badań laboratoryjnych zleconych przez danego użytkownika.
     *
     * @param orderedById UUID użytkownika
     * @return lista wyników powiązanych z użytkownikiem zlecającym badanie
     */
    List<LabResult> findByOrderedBy_Id(UUID orderedById);

    /**
     * Pobiera listę wyników badań dla podanego typu testu.
     *
     * @param testName nazwa testu
     * @return lista wyników badań dla zadanego testu
     */
    List<LabResult> findByTestName(String testName);

    /**
     * Pobiera listę wyników badań wykonanych w podanym zakresie dat.
     *
     * @param startDate początkowa data wyników
     * @param endDate   końcowa data wyników
     * @return lista wyników badań wykonanych w podanym zakresie dat
     */
    List<LabResult> findByResultDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera listę wyników badań laboratoryjnych o danym statusie.
     *
     * @param status status badania (np. "completed", "pending")
     * @return lista wyników badań o określonym statusie
     */
    List<LabResult> findByStatus(String status);

    /**
     * Pobiera listę wyników badań, których wynik zawiera podany ciąg znaków.
     *
     * @param resultFragment fragment wyniku
     * @return lista badań zawierających określony fragment w wynikach
     */
    List<LabResult> findByResultContaining(String resultFragment);

}