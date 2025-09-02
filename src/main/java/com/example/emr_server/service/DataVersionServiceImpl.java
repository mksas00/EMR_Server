package com.example.emr_server.service;

import com.example.emr_server.entity.DataVersion;
import com.example.emr_server.entity.User;
import com.example.emr_server.security.SecurityUtil;
import com.example.emr_server.repository.DataVersionRepository;
import com.example.emr_server.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DataVersionServiceImpl implements DataVersionService {

    private final DataVersionRepository dataVersionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DataVersionServiceImpl(DataVersionRepository dataVersionRepository,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.dataVersionRepository = dataVersionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private User current() { return SecurityUtil.getCurrentUser(userRepository).orElse(null); }
    private void assertAdmin() {
        User u = current();
        if (u == null) throw new SecurityException("Brak uwierzytelnienia");
        if (!"admin".equalsIgnoreCase(u.getRole())) throw new SecurityException("Brak uprawnień (wymagana rola admin)" );
    }

    @Override
    public List<DataVersion> getDataVersionsByEntityType(String entityType) {
        assertAdmin();
        return dataVersionRepository.findByEntityType(entityType);
    }

    @Override
    public List<DataVersion> getDataVersionsByEntityId(UUID entityId) {
        assertAdmin();
        return dataVersionRepository.findByEntityId(entityId);
    }

    @Override
    public List<DataVersion> getDataVersionsByModifiedBy(UUID modifiedById) {
        assertAdmin();
        return dataVersionRepository.findByModifiedBy_Id(modifiedById);
    }

    @Override
    public List<DataVersion> getDataVersionsByModifiedAtRange(Instant startTime, Instant endTime) {
        assertAdmin();
        return dataVersionRepository.findByModifiedAtBetween(startTime, endTime);
    }

    @Override
    public List<DataVersion> getDataVersionsByEntityTypeAndEntityId(String entityType, UUID entityId) {
        assertAdmin();
        return dataVersionRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public DataVersion saveDataVersion(DataVersion dataVersion) {
        assertAdmin();
        boolean create = dataVersion.getId() == null;
        DataVersion saved = dataVersionRepository.save(dataVersion);
        auditService.logPatient(current(), null, create?"CREATE_DATA_VERSION":"UPDATE_DATA_VERSION", "entityType="+saved.getEntityType()+", entityId="+saved.getEntityId());
        return saved;
    }

    @Override
    public void deleteDataVersionById(UUID dataVersionId) {
        assertAdmin();
        dataVersionRepository.findById(dataVersionId).ifPresent(v -> {
            dataVersionRepository.delete(v);
            auditService.logPatient(current(), null, "DELETE_DATA_VERSION", "entityType="+v.getEntityType()+", entityId="+v.getEntityId());
        });
    }
}