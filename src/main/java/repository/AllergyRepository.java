package repository;

import entity.Allergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllergyRepository extends JpaRepository<Allergy, UUID> {

    /**
     * Pobiera listę wszystkich alergii dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista alergii powiązanych z pacjentem
     */
    List<Allergy> findByPatient_Id(UUID patientId);

    /**
     * Pobiera listę alergii po nazwie alergenu.
     *
     * @param allergen nazwa alergenu
     * @return lista alergii związanych z podanym alergenem
     */
    List<Allergy> findByAllergen(String allergen);

    /**
     * Pobiera listę alergii o określonej ciężkości.
     *
     * @param severity ciężkość alergii
     * @return lista alergii o określonej ciężkości
     */
    List<Allergy> findBySeverity(String severity);

    /**
     * Pobiera listę alergii zanotowanych przez użytkownika.
     *
     * @param userId UUID użytkownika, który zanotował alergie
     * @return lista alergii zanotowanych przez użytkownika
     */
    List<Allergy> findByNotedBy_Id(UUID userId);
}