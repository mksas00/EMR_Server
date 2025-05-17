package service;

import entity.Patient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientService {

    /**
     * Pobiera listę pacjentów z podanym imieniem.
     *
     * @param firstName imię pacjenta
     * @return lista pacjentów o wskazanym imieniu
     */
    List<Patient> getPatientsByFirstName(String firstName);

    /**
     * Pobiera listę pacjentów z podanym nazwiskiem.
     *
     * @param lastName nazwisko pacjenta
     * @return lista pacjentów o wskazanym nazwisku
     */
    List<Patient> getPatientsByLastName(String lastName);

    /**
     * Pobiera listę pacjentów na podstawie imienia i nazwiska.
     *
     * @param firstName imię pacjenta
     * @param lastName  nazwisko pacjenta
     * @return lista pacjentów o podanym imieniu i nazwisku
     */
    List<Patient> getPatientsByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Pobiera pacjenta na podstawie numeru PESEL.
     *
     * @param pesel numer PESEL pacjenta
     * @return opcjonalny pacjent z danym numerem PESEL
     */
    Optional<Patient> getPatientByPesel(String pesel);

    /**
     * Pobiera listę pacjentów urodzonych w określonym przedziale dat.
     *
     * @param startDate data początkowa
     * @param endDate   data końcowa
     * @return lista pacjentów urodzonych w podanym zakresie dat
     */
    List<Patient> getPatientsByDateOfBirthBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Pobiera listę pacjentów utworzonych przez konkretnego użytkownika.
     *
     * @param createdById UUID użytkownika
     * @return lista pacjentów utworzonych przez danego użytkownika
     */
    List<Patient> getPatientsByCreatedBy(UUID createdById);

    /**
     * Wyszukuje pacjentów na podstawie fragmentu adresu.
     *
     * @param addressFragment fragment adresu pacjenta
     * @return lista pacjentów, których adres zawiera określony fragment
     */
    List<Patient> getPatientsByAddress(String addressFragment);

    /**
     * Pobiera listę pacjentów z określonej płci.
     *
     * @param gender płeć pacjenta
     * @return lista pacjentów o podanej płci
     */
    List<Patient> getPatientsByGender(String gender);

    /**
     * Pobiera listę pacjentów na podstawie fragmentu imienia lub nazwiska.
     *
     * @param nameFragment fragment imienia lub nazwiska pacjenta
     * @return lista pacjentów, których imię lub nazwisko zawiera określony fragment
     */
    List<Patient> getPatientsByNameFragment(String nameFragment);

    /**
     * Zapisuje nowego pacjenta lub aktualizuje istniejącego.
     *
     * @param patient obiekt pacjenta do zapisania
     * @return zapisany obiekt pacjenta
     */
    Patient savePatient(Patient patient);

    /**
     * Usuwa pacjenta na podstawie jego identyfikatora.
     *
     * @param patientId UUID pacjenta do usunięcia
     */
    void deletePatientById(UUID patientId);
}