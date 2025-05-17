package service;

import entity.MedicalRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.MedicalRecordRepository;
import service.MedicalRecordService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Override
    public List<MedicalRecord> getMedicalRecordsByPatientId(UUID patientId) {
        return medicalRecordRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByCreatedBy(UUID createdById) {
        return medicalRecordRepository.findByCreatedBy_Id(createdById);
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByType(String recordType) {
        return medicalRecordRepository.findByRecordType(recordType);
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByCreationRange(Instant startTimestamp, Instant endTimestamp) {
        return medicalRecordRepository.findByCreatedAtBetween(startTimestamp, endTimestamp);
    }

    @Override
    public List<MedicalRecord> getMedicalRecordsByPatientIdAndType(UUID patientId, String recordType) {
        return medicalRecordRepository.findByPatient_IdAndRecordType(patientId, recordType);
    }

    @Override
    public MedicalRecord saveMedicalRecord(MedicalRecord medicalRecord) {
        return medicalRecordRepository.save(medicalRecord);
    }

    @Override
    public void deleteMedicalRecordById(UUID medicalRecordId) {
        medicalRecordRepository.deleteById(medicalRecordId);
    }
}