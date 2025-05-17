package repository;

import entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {

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

}