package service;

import entity.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.AuditLogRepository;

import java.time.Instant;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Override
    public List<AuditLog> getLogsByUserId(Integer userId) {
        return auditLogRepository.findByUser_Id(userId);
    }

    @Override
    public List<AuditLog> getLogsByPatientId(Integer patientId) {
        return auditLogRepository.findByPatient_Id(patientId);
    }

    @Override
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    @Override
    public List<AuditLog> getLogsByTimestampRange(Instant startTimestamp, Instant endTimestamp) {
        return auditLogRepository.findByTimestampBetween(startTimestamp, endTimestamp);
    }

    @Override
    public List<AuditLog> getLogsByDescriptionFragment(String descriptionFragment) {
        return auditLogRepository.findByDescriptionContaining(descriptionFragment);
    }

    @Override
    public AuditLog saveLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    public void deleteLogById(Integer logId) {
        auditLogRepository.deleteById(logId);
    }
}