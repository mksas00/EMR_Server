package service;

import entity.Prescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.PrescriptionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Override
    public List<Prescription> getPrescriptionsByPatientId(UUID patientId) {
        return prescriptionRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<Prescription> getPrescriptionsByDoctorId(UUID doctorId) {
        return prescriptionRepository.findByDoctor_Id(doctorId);
    }

    @Override
    public List<Prescription> getActivePrescriptions(LocalDate currentDate) {
        return prescriptionRepository.findByExpirationDateAfter(currentDate);
    }

    @Override
    public List<Prescription> getPrescriptionsByMedication(String medication) {
        return prescriptionRepository.findByMedicationContaining(medication);
    }

    @Override
    public List<Prescription> getPrescriptionsByIssuedDateRange(LocalDate start, LocalDate end) {
        return prescriptionRepository.findByIssuedDateBetween(start, end);
    }

    @Override
    public Prescription savePrescription(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    @Override
    public void deletePrescriptionById(UUID prescriptionId) {
        prescriptionRepository.deleteById(prescriptionId);
    }
}