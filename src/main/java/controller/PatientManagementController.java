package controller;

import entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientManagementController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private VisitService visitService;

    @Autowired
    private AllergyService allergyService;

    @Autowired
    private LabResultService labResultService;

    @Autowired
    private MedicationHistoryService medicationHistoryService;

    @Autowired
    private ChronicDiseaseService chronicDiseaseService;

    @Autowired
    private MedicalFileService medicalFileService;

    @Autowired
    private PrescriptionService prescriptionService;

    /**
     * Pobierz szczegóły pacjenta oraz wszystkie powiązane dane.
     *
     * @param patientId UUID pacjenta
     * @return Dane pacjenta oraz pełna lista związanych informacji
     */
    @GetMapping("/{patientId}/details")
    public ResponseEntity<?> getPatientDetails(@PathVariable UUID patientId) {
        Optional<Patient> patient = patientService.getPatientsByCreatedBy(patientId).stream().findFirst();
        if (patient.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Visit> visits = visitService.getVisitsByPatientId(patientId);
        List<Allergy> allergies = allergyService.getAllergiesByPatientId(patientId);
        List<LabResult> labResults = labResultService.getLabResultsByPatientId(patientId);
        List<MedicationHistory> medicationHistory = medicationHistoryService.getMedicationHistoryByPatientId(patientId);
        List<ChronicDisease> diseases = chronicDiseaseService.getDiseasesByPatientId(patientId);
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatientId(patientId);
        List<MedicalFile> medicalFiles = medicalFileService.getMedicalFilesByPatientId(patientId);

        return ResponseEntity.ok(new Object() {
            public Patient patientDetails = patient.get();
            public List<Visit> visitHistory = visits;
            public List<Allergy> allergyHistory = allergies;
            public List<LabResult> labResultsHistory = labResults;
            public List<MedicationHistory> medicationHistories = medicationHistory;
            public List<ChronicDisease> chronicDiseases = diseases;
            public List<Prescription> prescriptionsList = prescriptions;
            public List<MedicalFile> files = medicalFiles;
        });
    }

    /**
     * Dodaj nowego pacjenta oraz opcjonalne szczegóły (wizyty, alergie, historia leków).
     *
     * @param patient             Dane pacjenta
     * @param visits              Lista wizyt pacjenta
     * @param allergies           Lista alergii pacjenta
     * @param medicationHistories Lista historii leków pacjenta
     * @return Zapisany pacjent z powiązanymi szczegółami
     */
    @PostMapping
    public ResponseEntity<?> savePatientWithDetails(@RequestBody Patient patient,
                                                    @RequestBody(required = false) List<Visit> visits,
                                                    @RequestBody(required = false) List<Allergy> allergies,
                                                    @RequestBody(required = false) List<MedicationHistory> medicationHistories) {
        // Zapisz pacjenta
        Patient savedPatient = patientService.savePatient(patient);

        // Zapisz wizyty
        if (visits != null) {
            visits.forEach(visit -> {
                visit.setPatient(savedPatient);
                visitService.saveVisit(visit);
            });
        }

        // Zapisz alergie
        if (allergies != null) {
            allergies.forEach(allergy -> {
                allergy.setPatient(savedPatient);
                allergyService.saveAllergy(allergy);
            });
        }

        // Zapisz historię leków
        if (medicationHistories != null) {
            medicationHistories.forEach(medication -> {
                medication.setPatient(savedPatient);
                medicationHistoryService.saveMedicationHistory(medication);
            });
        }

        return ResponseEntity.ok(savedPatient);
    }

    /**
     * Zaktualizuj dane pacjenta i powiązane informacje.
     *
     * @param patientId  UUID pacjenta
     * @param updatedPatient Nowe dane pacjenta
     * @return Zaktualizowany pacjent
     */
    @PutMapping("/{patientId}")
    public ResponseEntity<?> updatePatient(@PathVariable UUID patientId,
                                           @RequestBody Patient updatedPatient) {
        Optional<Patient> existingPatient = patientService.getPatientsByCreatedBy(patientId).stream().findFirst();
        if (existingPatient.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        updatedPatient.setId(patientId); // Ustaw identyfikator istniejącego pacjenta
        Patient savedPatient = patientService.savePatient(updatedPatient);
        return ResponseEntity.ok(savedPatient);
    }

    /**
     * Usuń pacjenta oraz wszystkie powiązane z nim dane.
     *
     * @param patientId UUID pacjenta
     * @return Kod 204 (No Content) po pomyślnym usunięciu
     */
    @DeleteMapping("/{patientId}")
    public ResponseEntity<?> deletePatientWithAllData(@PathVariable UUID patientId) {
        // Usuń wizyty
        List<Visit> visits = visitService.getVisitsByPatientId(patientId);
        visits.forEach(visit -> visitService.deleteVisit(visit.getId()));

        // Usuń alergie
        List<Allergy> allergies = allergyService.getAllergiesByPatientId(patientId);
        allergies.forEach(allergy -> allergyService.deleteAllergy(allergy.getId()));

        // Usuń historię leków
        List<MedicationHistory> medicationHistory = medicationHistoryService.getMedicationHistoryByPatientId(patientId);
        medicationHistory.forEach(history -> medicationHistoryService.deleteMedicationHistoryById(history.getId()));

        // Usuń dokumenty medyczne
        List<MedicalFile> medicalFiles = medicalFileService.getMedicalFilesByPatientId(patientId);
        medicalFiles.forEach(file -> medicalFileService.deleteMedicalFileById(file.getId()));

        // Usuń samego pacjenta
        patientService.deletePatientById(patientId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Pobierz listę pacjentów według fragmentu imienia/nazwiska.
     *
     * @param nameFragment Fragment imienia lub nazwiska
     * @return Lista pasujących pacjentów
     */
    @GetMapping("/search")
    public ResponseEntity<List<Patient>> searchPatientsByNameFragment(@RequestParam String nameFragment) {
        List<Patient> patients = patientService.getPatientsByNameFragment(nameFragment);
        return ResponseEntity.ok(patients);
    }
}