package repository;

import entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    /**
     * Pobiera listę pacjentów z podanym imieniem.
     *
     * @param firstName imię pacjenta
     * @return lista pacjentów o wskazanym imieniu
     */
    List<Patient> findByFirstName(String firstName);

    /**
     * Pobiera listę pacjentów z podanym nazwiskiem.
     *
     * @param lastName nazwisko pacjenta
     * @return lista pacjentów o wskazanym nazwisku
     */
    List<Patient> findByLastName(String lastName);

    /**
     * Pobiera listę pacjentów na podstawie imienia i nazwiska.
     *
     * @param firstName imię pacjenta
     * @param lastName  nazwisko pacjenta
     * @return lista pacjentów o podanym imieniu i nazwisku
     */
    List<Patient> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Pobiera pacjenta na podstawie numeru PESEL.
     *
     * @param pesel numer PESEL pacjenta
     * @return pacjent z danym numerem PESEL, jeśli istnieje
     */
    Optional<Patient> findByPesel(String pesel);

    /**
     * Pobiera listę pacjentów urodzonych w określonym przedziale dat.
     *
     * @param startDate data początkowa
     * @param endDate   data końcowa
     * @return lista pacjentów urodzonych w podanym zakresie dat
     */
    List<Patient> findByDateOfBirthBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera listę pacjentów utworzonych przez konkretnego użytkownika.
     *
     * @param createdById UUID użytkownika, który utworzył pacjenta
     * @return lista pacjentów utworzonych przez danego użytkownika
     */
    List<Patient> findByCreatedBy_Id(UUID createdById);

    /**
     * Wyszukuje pacjentów na podstawie fragmentu adresu.
     *
     * @param addressFragment fragment adresu pacjenta
     * @return lista pacjentów, których adres zawiera określony fragment
     */
    List<Patient> findByAddressContaining(String addressFragment);

    /**
     * Pobiera listę pacjentów z określonej płci.
     *
     * @param gender płeć pacjenta (np. "male", "female")
     * @return lista pacjentów o podanej płci
     */
    List<Patient> findByGender(String gender);

    /**
     * Pobiera listę pacjentów na podstawie fragmentu imienia lub nazwiska.
     *
     * @param nameFragment fragment imienia lub nazwiska pacjenta
     * @return lista pacjentów, których imię lub nazwisko zawiera określony fragment
     */
    List<Patient> findByFirstNameContainingOrLastNameContaining(String nameFragment1, String nameFragment2);
}