package com.example.emr_server.service;

import com.example.emr_server.entity.AuditLog;
import com.example.emr_server.entity.Patient;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPatient(User user, Patient patient, String action, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setUser(user);
            log.setPatient(patient);
            log.setAction(action);
            log.setDescription(description);
            log.setTimestamp(Instant.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {
            // celowo nie przerywamy głównej transakcji
        }
    }

    // Nowe: ogólne logowanie bez powiązania z pacjentem
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, String action, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setUser(user);
            log.setAction(action);
            log.setDescription(description);
            log.setTimestamp(Instant.now());
            auditLogRepository.save(log);
        } catch (Exception ignored) {
            // nie przerywamy transakcji biznesowej
        }
    }

    public String diffPatient(Patient before, Patient after) {
        if (before == null || after == null) return "";
        List<String> changes = new ArrayList<>();
        cmp(changes, "firstName", before.getFirstName(), after.getFirstName());
        cmp(changes, "lastName", before.getLastName(), after.getLastName());
        cmp(changes, "pesel", mask(before.getPesel()), mask(after.getPesel()));
        cmp(changes, "dateOfBirth", String.valueOf(before.getDateOfBirth()), String.valueOf(after.getDateOfBirth()));
        cmp(changes, "gender", before.getGender(), after.getGender());
        cmp(changes, "address", redact(before.getAddress()), redact(after.getAddress()));
        cmp(changes, "contactInfo", safeMap(before.getContactInfo()), safeMap(after.getContactInfo()));
        return String.join(", ", changes);
    }

    private void cmp(List<String> out, String field, Object a, Object b) {
        if (!Objects.equals(a, b)) out.add(field + ": '" + shortVal(a) + "'->'" + shortVal(b) + "'");
    }
    private String shortVal(Object v) {
        if (v == null) return "null";
        String s = v.toString();
        if (s.length() > 40) return s.substring(0,37) + "...";
        return s;
    }
    private String mask(String v) {
        if (v == null || v.length() < 4) return v;
        return "***" + v.substring(v.length()-4);
    }
    private String redact(String v) { return v == null ? null : "<redacted>"; }
    private String safeMap(Map<String,Object> m) { return m == null ? null : "{...}"; }
}
