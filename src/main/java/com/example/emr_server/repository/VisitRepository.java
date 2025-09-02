package com.example.emr_server.repository;

import com.example.emr_server.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID>, JpaSpecificationExecutor<Visit> {

    // Wyszukiwanie wizyt po ID pacjenta
    List<Visit> findByPatient_Id(UUID patientId);

    // Wyszukiwanie wizyt po ID lekarza
    List<Visit> findByDoctor_Id(UUID doctorId);

    // Wyszukiwanie wizyt pomiędzy konkretnymi datami
    List<Visit> findByVisitDateBetween(Instant startDate, Instant endDate);

    // Wyszukiwanie wizyt na podstawie typu wizyty
    List<Visit> findByVisitType(String visitType);

    // Opcjonalne wyszukiwanie wizyt pacjenta jednocześnie z uwzględnieniem poufności
    List<Visit> findByPatient_IdAndIsConfidential(UUID patientId, Boolean isConfidential);

    // Wyszukiwanie pierwszej wizyty pacjenta po dacie wizyty (pierwsza najwcześniejsza wizyta)
    Optional<Visit> findFirstByPatient_IdOrderByVisitDateAsc(UUID patientId);

    // Wyszukiwanie wszystkich wizyt danego lekarza po diagnozie
    List<Visit> findByDoctor_IdAndDiagnosisContaining(UUID doctorId, String diagnosisKeyword);

    // Wyszukiwanie wizyt z powodów zawierających konkretny ciąg znaków (np. wyszukiwanie powodów wizyt)
    List<Visit> findByReasonContainingIgnoreCase(String reason);

    @Query("select v from Visit v where v.doctor.id = :doctorId and (v.status is null or v.status <> :excluded) and v.visitDate < :end and (v.endDate is null or v.endDate > :start)")
    List<Visit> findOverlapping(@Param("doctorId") UUID doctorId,
                                @Param("start") Instant start,
                                @Param("end") Instant end,
                                @Param("excluded") Visit.Status excluded);

    @Query("select v from Visit v left join fetch v.patient p left join fetch p.createdBy where v.id = :id")
    Optional<Visit> findByIdWithPatient(@Param("id") UUID id);
}