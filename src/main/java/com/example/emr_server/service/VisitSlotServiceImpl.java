package com.example.emr_server.service;

import com.example.emr_server.controller.dto.VisitSlotDto;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.entity.Visit;
import com.example.emr_server.entity.VisitSlot;
import com.example.emr_server.repository.PatientRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.repository.VisitRepository;
import com.example.emr_server.repository.VisitSlotRepository;
import com.example.emr_server.security.AuthorizationService;
import com.example.emr_server.security.SecurityUtil;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VisitSlotServiceImpl implements VisitSlotService {

    private final VisitSlotRepository slotRepository;
    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public VisitSlotServiceImpl(VisitSlotRepository slotRepository,
                                VisitRepository visitRepository,
                                UserRepository userRepository,
                                PatientRepository patientRepository,
                                AuthorizationService authorizationService,
                                AuditService auditService) {
        this.slotRepository = slotRepository;
        this.visitRepository = visitRepository;
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }

    private boolean isAdmin(User u) { return u != null && u.getRole() != null && u.getRole().equalsIgnoreCase("admin"); }
    private boolean isDoctor(User u) { return u != null && u.getRole() != null && u.getRole().equalsIgnoreCase("doctor"); }

    private static VisitSlotDto toDto(VisitSlot s) {
        return new VisitSlotDto(
                s.getId(),
                s.getDoctor()!=null? s.getDoctor().getId(): null,
                s.getStartTime(),
                s.getEndTime(),
                s.getStatus()!=null? s.getStatus().name(): null,
                s.getPatient()!=null? s.getPatient().getId(): null,
                s.getVisit()!=null? s.getVisit().getId(): null
        );
    }

    private void assertNoOverlap(UUID doctorId, Instant start, Instant end, UUID excludeId) {
        var overlaps = slotRepository.findOverlapping(doctorId, start, end);
        boolean conflict = overlaps.stream().anyMatch(s -> excludeId == null || !s.getId().equals(excludeId));
        if (conflict) throw new IllegalStateException("Kolizja slotu z istniejącym slotem");
    }

    @Override
    public List<VisitSlotDto> list(Optional<UUID> doctorId, Optional<Instant> start, Optional<Instant> end, Optional<String> status) {
        User u = current();
        UUID docFilter = doctorId.orElseGet(() -> isDoctor(u)? u.getId(): null);
        VisitSlot.Status st = status.filter(s -> !s.isBlank()).map(s -> {
            try { return VisitSlot.Status.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
        }).orElse(null);
        List<VisitSlotDto> list = slotRepository.search(docFilter, start.orElse(null), end.orElse(null), st).stream()
                .sorted(Comparator.comparing(VisitSlot::getStartTime))
                .map(VisitSlotServiceImpl::toDto)
                .toList();
        if (isAdmin(u)) return list;
        if (isDoctor(u)) return list.stream().filter(d -> d.doctorId()!=null && d.doctorId().equals(u.getId())).toList();
        return List.of();
    }

    @Override
    public VisitSlotDto createForCurrent(VisitSlot slot) {
        User u = current();
        if (!(isDoctor(u) || isAdmin(u))) throw new SecurityException("Brak uprawnień do tworzenia slotów");
        slot.setDoctor(isDoctor(u)? u : slot.getDoctor()!=null? slot.getDoctor() : u);
        if (slot.getStartTime()==null || slot.getEndTime()==null || !slot.getStartTime().isBefore(slot.getEndTime())) {
            throw new IllegalArgumentException("Nieprawidłowe ramy czasowe slotu");
        }
        slot.setStatus(slot.getStatus()==null? VisitSlot.Status.AVAILABLE : slot.getStatus());
        assertNoOverlap(slot.getDoctor().getId(), slot.getStartTime(), slot.getEndTime(), null);
        var saved = slotRepository.save(slot);
        auditService.log(u, "CREATE_SLOT", "slotId="+saved.getId());
        return toDto(saved);
    }

    @Override
    public Optional<VisitSlotDto> reserve(UUID slotId, UUID patientId) {
        User u = current();
        return slotRepository.findById(slotId).flatMap(s -> {
            Patient p = patientRepository.findById(patientId).orElse(null);
            if (p==null) return Optional.empty();
            if (!authorizationService.canWritePatient(u, p)) throw new SecurityException("Brak uprawnień do rezerwacji dla pacjenta");
            if (s.getStatus()!= VisitSlot.Status.AVAILABLE) throw new IllegalStateException("Slot nie jest dostępny");
            // Kolizje z istniejącymi wizytami lekarza
            var vOverlaps = visitRepository.findOverlapping(s.getDoctor().getId(), s.getStartTime(), s.getEndTime(), Visit.Status.CANCELED);
            if (!vOverlaps.isEmpty()) throw new IllegalStateException("Kolizja z istniejącą wizytą lekarza");
            // Utwórz wizytę i podepnij do slotu
            Visit v = new Visit();
            v.setPatient(p);
            v.setDoctor(s.getDoctor());
            v.setVisitDate(s.getStartTime());
            v.setEndDate(s.getEndTime());
            v.setVisitType("APPOINTMENT");
            v.setStatus(Visit.Status.CONFIRMED);
            var savedVisit = visitRepository.save(v);
            auditService.logPatient(u, p, "CREATE_VISIT_FROM_SLOT", "visitId="+savedVisit.getId()+", slotId="+s.getId());
            s.setPatient(p);
            s.setStatus(VisitSlot.Status.RESERVED);
            s.setVisit(savedVisit);
            var saved = slotRepository.save(s);
            return Optional.of(toDto(saved));
        });
    }

    @Override
    public Optional<VisitSlotDto> release(UUID slotId) {
        User u = current();
        return slotRepository.findById(slotId).map(s -> {
            if (!(isAdmin(u) || (isDoctor(u) && s.getDoctor()!=null && s.getDoctor().getId().equals(u.getId()))))
                throw new SecurityException("Brak uprawnień do zwolnienia slotu");
            // Jeśli istnieje powiązana wizyta, anuluj ją
            if (s.getVisit()!=null) {
                var v = s.getVisit();
                v.setStatus(Visit.Status.CANCELED);
                visitRepository.save(v);
                auditService.logPatient(u, v.getPatient(), "CANCEL_VISIT_FROM_SLOT_RELEASE", "visitId="+v.getId()+", slotId="+s.getId());
                s.setVisit(null);
            }
            s.setPatient(null);
            s.setStatus(VisitSlot.Status.AVAILABLE);
            var saved = slotRepository.save(s);
            auditService.log(u, "RELEASE_SLOT", "slotId="+saved.getId());
            return toDto(saved);
        });
    }

    @Override
    public boolean delete(UUID slotId) {
        User u = current();
        return slotRepository.findById(slotId).map(s -> {
            if (!(isAdmin(u) || (isDoctor(u) && s.getDoctor()!=null && s.getDoctor().getId().equals(u.getId()))))
                throw new SecurityException("Brak uprawnień do usunięcia slotu");
            if (s.getStatus()== VisitSlot.Status.RESERVED) throw new IllegalStateException("Nie można usunąć zarezerwowanego slotu");
            slotRepository.delete(s);
            auditService.log(u, "DELETE_SLOT", "slotId="+s.getId());
            return true;
        }).orElse(false);
    }
}
