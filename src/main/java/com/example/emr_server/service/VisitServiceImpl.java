package com.example.emr_server.service;

import com.example.emr_server.entity.Visit;
import com.example.emr_server.entity.User;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.VisitRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.PatientRepository;
import com.example.emr_server.controller.dto.VisitDto;
import com.example.emr_server.repository.spec.VisitSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;
    private final AuthorizationService authorizationService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PatientRepository patientRepository;

    public VisitServiceImpl(VisitRepository visitRepository,
                            AuthorizationService authorizationService,
                            UserRepository userRepository,
                            AuditService auditService,
                            PatientRepository patientRepository) {
        this.visitRepository = visitRepository;
        this.authorizationService = authorizationService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.patientRepository = patientRepository;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private VisitDto toDto(Visit v) {
        return new VisitDto(
                v.getId(),
                v.getPatient() != null ? v.getPatient().getId() : null,
                v.getDoctor() != null ? v.getDoctor().getId() : null,
                v.getVisitDate(),
                v.getEndDate(),
                v.getVisitType(),
                v.getReason(),
                v.getDiagnosis(),
                v.getNotes(),
                v.getConfidential(),
                v.getStatus() != null ? v.getStatus().name() : null
        );
    }

    @Override
    public Visit getVisitById(UUID id) {
        User u = current();
        return visitRepository.findById(id)
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .orElse(null);
    }

    @Override
    public List<Visit> getVisitsByPatientId(UUID patientId) {
        User u = current();
        return visitRepository.findByPatient_Id(patientId).stream()
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Visit> getVisitsByDoctorId(UUID doctorId) {
        User u = current();
        return visitRepository.findByDoctor_Id(doctorId).stream()
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Visit> getVisitsByDateRange(Instant start, Instant end) {
        User u = current();
        return visitRepository.findByVisitDateBetween(start, end).stream()
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .collect(Collectors.toList());
    }

    private void assertNoCollision(User doctor, Instant start, Instant end, UUID excludeId) {
        Instant effStart = start;
        Instant effEnd = end != null ? end : start.plus(Duration.ofMinutes(30));
        var overlaps = visitRepository.findOverlapping(doctor.getId(), effStart, effEnd, Visit.Status.CANCELED);
        boolean conflict = overlaps.stream().anyMatch(v -> excludeId == null || !v.getId().equals(excludeId));
        if (conflict) {
            throw new IllegalStateException("Kolizja terminu z inną wizytą lekarza");
        }
    }

    @Override
    public Visit saveVisit(Visit visit) {
        User u = current();
        if (!authorizationService.canWritePatient(u, visit.getPatient()))
            throw new SecurityException("Brak uprawnień do zapisu wizyty");
        if (visit.getDoctor() == null) visit.setDoctor(u);
        if (visit.getStatus() == null) visit.setStatus(Visit.Status.PLANNED);
        assertNoCollision(visit.getDoctor(), visit.getVisitDate(), visit.getEndDate(), visit.getId());
        boolean create = visit.getId()==null;
        Visit saved = visitRepository.save(visit);
        auditService.logPatient(u, saved.getPatient(), create?"CREATE_VISIT":"UPDATE_VISIT", "visitId="+saved.getId());
        return saved;
    }

    @Override
    public void deleteVisit(UUID id) {
        visitRepository.findById(id).ifPresent(v -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, v.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia wizyty");
            visitRepository.delete(v);
            auditService.logPatient(u, v.getPatient(), "DELETE_VISIT", "visitId="+v.getId());
        });
    }

    @Override
    public List<Visit> findAll() {
        User u = current();
        return visitRepository.findAll().stream()
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Visit> findById(UUID id) {
        User u = current();
        return visitRepository.findById(id)
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()));
    }

    @Override
    public Visit save(Visit visit) { return saveVisit(visit); }

    @Override
    public boolean existsById(UUID id) { return visitRepository.existsById(id); }

    @Override
    public void deleteById(UUID id) { deleteVisit(id); }

    @Override
    public Optional<Visit> updateVisit(UUID id, Visit update) {
        return visitRepository.findById(id).map(existing -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji wizyty");
            existing.setVisitDate(update.getVisitDate());
            existing.setEndDate(update.getEndDate());
            existing.setVisitType(update.getVisitType());
            existing.setReason(update.getReason());
            existing.setDiagnosis(update.getDiagnosis());
            existing.setNotes(update.getNotes());
            existing.setConfidential(update.getConfidential());
            existing.setStatus(update.getStatus() != null ? update.getStatus() : existing.getStatus());
            assertNoCollision(existing.getDoctor(), existing.getVisitDate(), existing.getEndDate(), existing.getId());
            Visit saved = visitRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_VISIT", "visitId="+saved.getId());
            return saved;
        });
    }

    // Nowe metody DTO + autoryzacja
    @Override
    public List<VisitDto> getAllVisibleForCurrent() {
        return findAll().stream().map(this::toDto).toList();
    }

    @Override
    public Optional<VisitDto> getVisibleById(UUID id) {
        return findById(id).map(this::toDto);
    }

    @Override
    public VisitDto createForCurrent(UUID patientId, Visit visit) {
        User u = current();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Pacjent nie istnieje"));
        if (!authorizationService.canWritePatient(u, patient))
            throw new SecurityException("Brak uprawnień do utworzenia wizyty");
        visit.setPatient(patient);
        if (visit.getDoctor() == null) visit.setDoctor(u);
        if (visit.getStatus() == null) visit.setStatus(Visit.Status.PLANNED);
        assertNoCollision(visit.getDoctor(), visit.getVisitDate(), visit.getEndDate(), null);
        Visit saved = visitRepository.save(visit);
        auditService.logPatient(u, saved.getPatient(), "CREATE_VISIT", "visitId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<VisitDto> updateForCurrent(UUID id, Visit update) {
        return visitRepository.findById(id).map(existing -> {
            User u = current();
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do aktualizacji wizyty");
            existing.setVisitDate(update.getVisitDate());
            existing.setEndDate(update.getEndDate());
            existing.setVisitType(update.getVisitType());
            existing.setReason(update.getReason());
            existing.setDiagnosis(update.getDiagnosis());
            existing.setNotes(update.getNotes());
            existing.setConfidential(update.getConfidential());
            existing.setStatus(update.getStatus() != null ? update.getStatus() : existing.getStatus());
            assertNoCollision(existing.getDoctor(), existing.getVisitDate(), existing.getEndDate(), existing.getId());
            Visit saved = visitRepository.save(existing);
            auditService.logPatient(u, saved.getPatient(), "UPDATE_VISIT", "visitId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean deleteForCurrent(UUID id) {
        User u = current();
        return visitRepository.findById(id).map(existing -> {
            if (!authorizationService.canWritePatient(u, existing.getPatient()))
                throw new SecurityException("Brak uprawnień do usunięcia wizyty");
            visitRepository.delete(existing);
            auditService.logPatient(u, existing.getPatient(), "DELETE_VISIT", "visitId="+existing.getId());
            return true;
        }).orElse(false);
    }

    @Override
    public Page<VisitDto> searchVisible(
            java.util.Optional<java.util.UUID> patientId,
            java.util.Optional<java.util.UUID> doctorId,
            java.util.Optional<java.time.Instant> start,
            java.util.Optional<java.time.Instant> end,
            java.util.Optional<String> type,
            java.util.Optional<String> diagnosis,
            java.util.Optional<String> reason,
            java.util.Optional<Boolean> confidential,
            java.util.Optional<String> status,
            Pageable pageable
    ) {
        User u = current();
        var spec = VisitSpecifications.withFilters(patientId, doctorId, start, end, type, diagnosis, reason, confidential, status);
        var all = visitRepository.findAll(spec);
        var visible = all.stream()
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .map(this::toDto)
                .toList();
        return com.example.emr_server.util.PageUtils.paginate(visible, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> exportIcs(UUID visitId) {
        User u = current();
        return visitRepository.findByIdWithPatient(visitId)
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .map(v -> {
                    var now = Instant.now();
                    var fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
                    var start = fmt.format(v.getVisitDate());
                    var end = fmt.format(v.getEndDate() != null ? v.getEndDate() : v.getVisitDate().plus(Duration.ofMinutes(30)));
                    String status;
                    if (v.getStatus() == Visit.Status.CANCELED) status = "CANCELLED";
                    else if (v.getStatus() == Visit.Status.PLANNED) status = "TENTATIVE";
                    else status = "CONFIRMED";
                    String summary = (v.getVisitType() != null && !v.getVisitType().isBlank()) ? v.getVisitType() : "Wizyta";
                    String description;
                    if (Boolean.TRUE.equals(v.getConfidential())) {
                        description = "Szczegóły poufne";
                    } else {
                        description = (v.getReason()!=null? "Powód: "+v.getReason(): "") +
                                      (v.getDiagnosis()!=null? (v.getReason()!=null? "\\n": "") + "Diagnoza: "+v.getDiagnosis(): "");
                        if (description.isBlank()) description = "Wizyta medyczna";
                    }
                    String ics = "BEGIN:VCALENDAR\r\n" +
                            "VERSION:2.0\r\n" +
                            "PRODID:-//emr_server//EN\r\n" +
                            "CALSCALE:GREGORIAN\r\n" +
                            "METHOD:PUBLISH\r\n" +
                            "BEGIN:VEVENT\r\n" +
                            "UID:" + v.getId() + "@emr.local\r\n" +
                            "DTSTAMP:" + fmt.format(now) + "\r\n" +
                            "DTSTART:" + start + "\r\n" +
                            "DTEND:" + end + "\r\n" +
                            "SUMMARY:" + escapeIcs(summary) + "\r\n" +
                            "DESCRIPTION:" + escapeIcs(description) + "\r\n" +
                            "STATUS:" + status + "\r\n" +
                            "END:VEVENT\r\n" +
                            "END:VCALENDAR\r\n";
                    return ics;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCalendarIcs(
            java.util.Optional<java.util.UUID> doctorId,
            java.util.Optional<java.time.Instant> start,
            java.util.Optional<java.time.Instant> end,
            java.util.Optional<String> status
    ) {
        User u = current();
        var spec = VisitSpecifications.withFilters(
                java.util.Optional.empty(),
                doctorId,
                start,
                end,
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                status
        );
        var fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
        var visits = visitRepository.findAll(spec).stream()
                .filter(v -> authorizationService.canReadPatient(u, v.getPatient()))
                .sorted(Comparator.comparing(Visit::getVisitDate))
                .toList();
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n")
          .append("VERSION:2.0\r\n")
          .append("PRODID:-//emr_server//EN\r\n")
          .append("CALSCALE:GREGORIAN\r\n")
          .append("METHOD:PUBLISH\r\n");
        var now = Instant.now();
        for (var v : visits) {
            String veStatus;
            if (v.getStatus() == Visit.Status.CANCELED) veStatus = "CANCELLED";
            else if (v.getStatus() == Visit.Status.PLANNED) veStatus = "TENTATIVE";
            else veStatus = "CONFIRMED";
            String summary = (v.getVisitType() != null && !v.getVisitType().isBlank()) ? v.getVisitType() : "Wizyta";
            String description;
            if (Boolean.TRUE.equals(v.getConfidential())) {
                description = "Szczegóły poufne";
            } else {
                description = (v.getReason()!=null? "Powód: "+v.getReason(): "") +
                              (v.getDiagnosis()!=null? (v.getReason()!=null? "\\n": "") + "Diagnoza: "+v.getDiagnosis(): "");
                if (description.isBlank()) description = "Wizyta medyczna";
            }
            String dtStart = fmt.format(v.getVisitDate());
            String dtEnd = fmt.format(v.getEndDate() != null ? v.getEndDate() : v.getVisitDate().plus(Duration.ofMinutes(30)));
            sb.append("BEGIN:VEVENT\r\n")
              .append("UID:").append(v.getId()).append("@emr.local\r\n")
              .append("DTSTAMP:").append(fmt.format(now)).append("\r\n")
              .append("DTSTART:").append(dtStart).append("\r\n")
              .append("DTEND:").append(dtEnd).append("\r\n")
              .append("SUMMARY:").append(escapeIcs(summary)).append("\r\n")
              .append("DESCRIPTION:").append(escapeIcs(description)).append("\r\n")
              .append("STATUS:").append(veStatus).append("\r\n")
              .append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String escapeIcs(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}