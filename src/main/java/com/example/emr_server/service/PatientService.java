package com.example.emr_server.service;

import com.example.emr_server.controller.dto.PatientDto;
import com.example.emr_server.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Pobiera listę pacjentów z określonej płci.
     *
     * @param gender płeć pacjenta
     * @return lista pacjentów o podanej płci
     */
    List<Patient> getPatientsByGender(String gender);

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

    /**
     * Pobiera opcjonalnego pacjenta na podstawie jego identyfikatora.
     *
     * @param patientId UUID pacjenta do znalezienia
     * @return opcjonalny pacjent o wskazanym identyfikatorze
     */
    Optional<Patient> findPatientById(UUID patientId);

    /**
     * Aktualizuje dane pacjenta na podstawie jego identyfikatora.
     *
     * @param id      UUID pacjenta do aktualizacji
     * @param update  obiekt pacjenta z zaktualizowanymi danymi
     * @return opcjonalny zaktualizowany pacjent
     */
    Optional<Patient> updatePatient(UUID id, Patient update);

    List<Patient> findAll();

    boolean existsById(UUID id);

    /**
     * Pobiera listę pacjentów widocznych dla aktualnego użytkownika.
     *
     * @return lista pacjentów widocznych dla aktualnego użytkownika
     */
    List<PatientDto> getAllVisibleForCurrent();

    /**
     * Pobiera pacjenta widocznego dla aktualnego użytkownika na podstawie jego identyfikatora.
     *
     * @param id UUID pacjenta do znalezienia
     * @return opcjonalny pacjent widoczny dla aktualnego użytkownika o wskazanym identyfikatorze
     */
    Optional<PatientDto> getVisibleById(UUID id);

    /**
     * Tworzy nowego pacjenta dla aktualnego użytkownika.
     *
     * @param patient obiekt pacjenta do utworzenia
     * @return utworzony obiekt pacjenta
     */
    PatientDto createForCurrent(Patient patient);

    /**
     * Aktualizuje dane pacjenta dla aktualnego użytkownika na podstawie jego identyfikatora.
     *
     * @param id      UUID pacjenta do aktualizacji
     * @param update  obiekt pacjenta z zaktualizowanymi danymi
     * @return opcjonalny zaktualizowany pacjent widoczny dla aktualnego użytkownika
     */
    Optional<PatientDto> updateForCurrent(UUID id, Patient update);

    /**
     * Usuwa pacjenta dla aktualnego użytkownika na podstawie jego identyfikatora.
     *
     * @param id UUID pacjenta do usunięcia
     * @return true, jeśli pacjent został usunięty, false w przeciwnym razie
     */
    boolean deleteForCurrent(UUID id);

    /**
     * Wyszukuje pacjentów widocznych dla aktualnego użytkownika na podstawie filtrów.
     *
     * @param firstName      opcjonalne imię pacjenta
     * @param lastName       opcjonalne nazwisko pacjenta
     * @param pesel          opcjonalny numer PESEL pacjenta
     * @param dobStart       opcjonalna data początkowa urodzenia
     * @param dobEnd         opcjonalna data końcowa urodzenia
     * @param gender         opcjonalna płeć pacjenta
     * @param addressFragment opcjonalny fragment adresu pacjenta
     * @param createdById    opcjonalny UUID użytkownika, który utworzył pacjenta
     * @param pageable        obiekt Pageable zawierający informacje o paginacji i sortowaniu
     * @return strona pacjentów spełniających podane kryteria
     */
    Page<PatientDto> searchVisible(
            Optional<String> firstName,
            Optional<String> lastName,
            Optional<String> pesel,
            Optional<LocalDate> dobStart,
            Optional<LocalDate> dobEnd,
            Optional<String> gender,
            Optional<String> addressFragment,
            Optional<UUID> createdById,
            Pageable pageable
    );
}