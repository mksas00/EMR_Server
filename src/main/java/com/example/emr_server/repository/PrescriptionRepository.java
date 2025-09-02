package com.example.emr_server.repository;

import com.example.emr_server.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID>, JpaSpecificationExecutor<Prescription> {

    /**
     * Pobiera listę recept danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista recept powiązanych z pacjentem
     */
    List<Prescription> findByPatient_Id(UUID patientId);

    /**
     * Pobiera listę recept wystawionych przez określonego lekarza.
     *
     * @param doctorId UUID lekarza
     * @return lista recept wystawionych przez danego lekarza
     */
    List<Prescription> findByDoctor_Id(UUID doctorId);

    /**
     * Pobiera listę aktywnych recept, które jeszcze nie wygasły.
     *
     * @param currentDate data dzisiejsza
     * @return lista ważnych recept
     */
    List<Prescription> findByExpirationDateAfter(LocalDate currentDate);


    /**
     * Pobiera listę recept wystawionych w określonym przedziale dat.
     *
     * @param start początek zakresu
     * @param end   koniec zakresu
     * @return lista recept wystawionych w tym czasie
     */
    List<Prescription> findByIssuedDateBetween(LocalDate start, LocalDate end);

}