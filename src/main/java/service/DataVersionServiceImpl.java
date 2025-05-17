package service;

import entity.DataVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.DataVersionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DataVersionServiceImpl implements DataVersionService {

    @Autowired
    private DataVersionRepository dataVersionRepository;

    @Override
    public List<DataVersion> getDataVersionsByEntityType(String entityType) {
        return dataVersionRepository.findByEntityType(entityType);
    }

    @Override
    public List<DataVersion> getDataVersionsByEntityId(UUID entityId) {
        return dataVersionRepository.findByEntityId(entityId);
    }

    @Override
    public List<DataVersion> getDataVersionsByModifiedBy(UUID modifiedById) {
        return dataVersionRepository.findByModifiedBy_Id(modifiedById);
    }

    @Override
    public List<DataVersion> getDataVersionsByModifiedAtRange(Instant startTime, Instant endTime) {
        return dataVersionRepository.findByModifiedAtBetween(startTime, endTime);
    }

    @Override
    public List<DataVersion> getDataVersionsByEntityTypeAndEntityId(String entityType, UUID entityId) {
        return dataVersionRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public DataVersion saveDataVersion(DataVersion dataVersion) {
        return dataVersionRepository.save(dataVersion);
    }

    @Override
    public void deleteDataVersionById(UUID dataVersionId) {
        dataVersionRepository.deleteById(dataVersionId);
    }
}