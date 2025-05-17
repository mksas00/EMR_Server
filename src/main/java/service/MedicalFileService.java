package service;

import entity.MedicalFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MedicalFileService {

    /**
     * Pobiera listę plików medycznych dla danego pacjenta.
     *
     * @param patientId UUID pacjenta
     * @return lista plików medycznych powiązanych z pacjentem
     */
    List<MedicalFile> getMedicalFilesByPatientId(UUID patientId);

    /**
     * Pobiera listę plików medycznych przesłanych przez określonego użytkownika.
     *
     * @param uploadedById UUID użytkownika
     * @return lista plików medycznych przesłanych przez użytkownika
     */
    List<MedicalFile> getMedicalFilesByUploadedBy(UUID uploadedById);

    /**
     * Pobiera listę plików medycznych o określonym typie MIME.
     *
     * @param mimeType typ MIME
     * @return lista plików o podanym typie MIME
     */
    List<MedicalFile> getMedicalFilesByMimeType(String mimeType);

    /**
     * Pobiera listę plików medycznych przesłanych w podanym zakresie czasowym.
     *
     * @param startTimestamp znacznik początkowego czasu
     * @param endTimestamp   znacznik końcowego czasu
     * @return lista plików przesłanych w danym zakresie czasowym
     */
    List<MedicalFile> getMedicalFilesByUploadedAtRange(Instant startTimestamp, Instant endTimestamp);

    /**
     * Pobiera listę plików medycznych dla danego pacjenta i typu MIME.
     *
     * @param patientId UUID pacjenta
     * @param mimeType  typ MIME
     * @return lista plików powiązanych z pacjentem i typem MIME
     */
    List<MedicalFile> getMedicalFilesByPatientIdAndMimeType(UUID patientId, String mimeType);

    /**
     * Pobiera listę plików na podstawie fragmentu nazwy pliku.
     *
     * @param fileNameFragment fragment nazwy pliku
     * @return lista plików, których nazwa zawiera podany fragment
     */
    List<MedicalFile> getMedicalFilesByFileNameFragment(String fileNameFragment);

    /**
     * Zapisuje nowy plik medyczny lub aktualizuje istniejący.
     *
     * @param medicalFile obiekt pliku medycznego do zapisania
     * @return zapisany obiekt pliku medycznego
     */
    MedicalFile saveMedicalFile(MedicalFile medicalFile);

    /**
     * Usuwa plik medyczny na podstawie jego identyfikatora.
     *
     * @param medicalFileId UUID pliku medycznego do usunięcia
     */
    void deleteMedicalFileById(UUID medicalFileId);
}