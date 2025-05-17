package service;

import entity.Visit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.VisitRepository;
import service.VisitService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VisitServiceImpl implements VisitService {

    @Autowired
    private VisitRepository visitRepository;

    @Override
    public Visit getVisitById(UUID id) {
        return visitRepository.findById(id).orElse(null);
    }

    @Override
    public List<Visit> getVisitsByPatientId(UUID patientId) {
        return visitRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<Visit> getVisitsByDoctorId(UUID doctorId) {
        return visitRepository.findByDoctor_Id(doctorId);
    }

    @Override
    public List<Visit> getVisitsByDateRange(Instant start, Instant end) {
        return visitRepository.findByVisitDateBetween(start, end);
    }

    @Override
    public Visit saveVisit(Visit visit) {
        return visitRepository.save(visit);
    }

    @Override
    public void deleteVisit(UUID id) {
        visitRepository.deleteById(id);
    }
}