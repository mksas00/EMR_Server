package com.example.emr_server.repository;

import com.example.emr_server.entity.MedicalFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicalFileRepository extends JpaRepository<MedicalFile, UUID>, JpaSpecificationExecutor<MedicalFile> {

    /**
     * Pobiera listę plików medycznych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista plików medycznych powiązanych z pacjentem
     */
    List<MedicalFile> findByPatient_Id(UUID patientId);

    /**
     * Pobiera listę plików medycznych przesłanych przez określonego użytkownika.
     *
     * @param uploadedById UUID użytkownika
     * @return lista plików medycznych przesłanych przez użytkownika
     */
    List<MedicalFile> findByUploadedBy_Id(UUID uploadedById);

    /**
     * Pobiera listę plików medycznych o określonym typie MIME.
     *
     * @param mimeType typ MIME (np. "application/pdf", "image/jpeg")
     * @return lista plików o podanym typie MIME
     */
    List<MedicalFile> findByMimeType(String mimeType);

    /**
     * Pobiera listę plików medycznych przesłanych w podanym zakresie czasowym.
     *
     * @param startTimestamp znacznik początkowego czasu
     * @param endTimestamp   znacznik końcowego czasu
     * @return lista plików przesłanych w określonym przedziale czasowym
     */
    List<MedicalFile> findByUploadedAtBetween(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera listę plików medycznych dla danego pacjenta i typu MIME.
     *
     * @param patientId UUID pacjenta
     * @param mimeType  typ MIME (np. "application/pdf", "image/jpeg")
     * @return lista plików powiązanych z pacjentem i typem MIME
     */
    List<MedicalFile> findByPatient_IdAndMimeType(UUID patientId, String mimeType);

    /**
     * Pobiera listę plików na podstawie fragmentu nazwy pliku.
     *
     * @param fileNameFragment fragment nazwy pliku
     * @return lista plików, których nazwa zawiera podany fragment
     */
    List<MedicalFile> findByFileNameContaining(String fileNameFragment);

}