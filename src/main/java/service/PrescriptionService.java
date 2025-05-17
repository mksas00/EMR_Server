package service;

import entity.Prescription;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PrescriptionService {

    /**
     * Pobiera listę recept danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista recept powiązanych z pacjentem
     */
    List<Prescription> getPrescriptionsByPatientId(UUID patientId);

    /**
     * Pobiera listę recept wystawionych przez określonego lekarza.
     *
     * @param doctorId UUID lekarza
     * @return lista recept wystawionych przez danego lekarza
     */
    List<Prescription> getPrescriptionsByDoctorId(UUID doctorId);

    /**
     * Pobiera listę aktywnych recept, które jeszcze nie wygasły.
     *
     * @param currentDate data dzisiejsza
     * @return lista ważnych recept
     */
    List<Prescription> getActivePrescriptions(LocalDate currentDate);

    /**
     * Pobiera listę recept na podstawie nazwy leku.
     *
     * @param medication nazwa leku
     * @return lista recept zawierających dany lek
     */
    List<Prescription> getPrescriptionsByMedication(String medication);

    /**
     * Pobiera listę recept wystawionych w określonym przedziale dat.
     *
     * @param start początek zakresu
     * @param end   koniec zakresu
     * @return lista recept wystawionych w tym czasie
     */
    List<Prescription> getPrescriptionsByIssuedDateRange(LocalDate start, LocalDate end);

    /**
     * Zapisuje nową receptę lub aktualizuje istniejącą.
     *
     * @param prescription obiekt recepty do zapisania
     * @return zapisany obiekt recepty
     */
    Prescription savePrescription(Prescription prescription);

    /**
     * Usuwa receptę na podstawie jej identyfikatora.
     *
     * @param prescriptionId UUID recepty do usunięcia
     */
    void deletePrescriptionById(UUID prescriptionId);
}