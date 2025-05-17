package service;

import entity.Visit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface VisitService {
    Visit getVisitById(UUID id);
    List<Visit> getVisitsByPatientId(UUID patientId);
    List<Visit> getVisitsByDoctorId(UUID doctorId);
    List<Visit> getVisitsByDateRange(Instant start, Instant end);
    Visit saveVisit(Visit visit);
    void deleteVisit(UUID id);
}