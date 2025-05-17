package service;

import entity.LabResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.LabResultRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class LabResultServiceImpl implements LabResultService {

    @Autowired
    private LabResultRepository labResultRepository;

    @Override
    public List<LabResult> getLabResultsByPatientId(UUID patientId) {
        return labResultRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<LabResult> getLabResultsByOrderedBy(UUID orderedById) {
        return labResultRepository.findByOrderedBy_Id(orderedById);
    }

    @Override
    public List<LabResult> getLabResultsByTestName(String testName) {
        return labResultRepository.findByTestName(testName);
    }

    @Override
    public List<LabResult> getLabResultsByDateRange(LocalDate startDate, LocalDate endDate) {
        return labResultRepository.findByResultDateBetween(startDate, endDate);
    }

    @Override
    public List<LabResult> getLabResultsByStatus(String status) {
        return labResultRepository.findByStatus(status);
    }

    @Override
    public List<LabResult> getLabResultsByResultFragment(String resultFragment) {
        return labResultRepository.findByResultContaining(resultFragment);
    }

    @Override
    public LabResult saveLabResult(LabResult labResult) {
        return labResultRepository.save(labResult);
    }

    @Override
    public void deleteLabResultById(UUID labResultId) {
        labResultRepository.deleteById(labResultId);
    }
}