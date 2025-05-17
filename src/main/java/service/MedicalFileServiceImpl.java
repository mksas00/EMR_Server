package service;

import entity.MedicalFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.MedicalFileRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MedicalFileServiceImpl implements MedicalFileService {

    @Autowired
    private MedicalFileRepository medicalFileRepository;

    @Override
    public List<MedicalFile> getMedicalFilesByPatientId(UUID patientId) {
        return medicalFileRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<MedicalFile> getMedicalFilesByUploadedBy(UUID uploadedById) {
        return medicalFileRepository.findByUploadedBy_Id(uploadedById);
    }

    @Override
    public List<MedicalFile> getMedicalFilesByMimeType(String mimeType) {
        return medicalFileRepository.findByMimeType(mimeType);
    }

    @Override
    public List<MedicalFile> getMedicalFilesByUploadedAtRange(Instant startTimestamp, Instant endTimestamp) {
        return medicalFileRepository.findByUploadedAtBetween(startTimestamp, endTimestamp);
    }

    @Override
    public List<MedicalFile> getMedicalFilesByPatientIdAndMimeType(UUID patientId, String mimeType) {
        return medicalFileRepository.findByPatient_IdAndMimeType(patientId, mimeType);
    }

    @Override
    public List<MedicalFile> getMedicalFilesByFileNameFragment(String fileNameFragment) {
        return medicalFileRepository.findByFileNameContaining(fileNameFragment);
    }

    @Override
    public MedicalFile saveMedicalFile(MedicalFile medicalFile) {
        return medicalFileRepository.save(medicalFile);
    }

    @Override
    public void deleteMedicalFileById(UUID medicalFileId) {
        medicalFileRepository.deleteById(medicalFileId);
    }
}