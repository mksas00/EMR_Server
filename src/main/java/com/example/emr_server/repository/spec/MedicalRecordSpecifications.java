package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.MedicalRecord;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class MedicalRecordSpecifications {
    private MedicalRecordSpecifications() {}

    public static Specification<MedicalRecord> withFilters(
            Optional<UUID> patientId,
            Optional<UUID> createdById,
            Optional<String> type,
            Optional<Instant> start,
            Optional<Instant> end
    ) {
        Specification<MedicalRecord> spec = Specification.where(null);
        if (patientId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("patient").get("id"), patientId.get()));
        }
        if (createdById.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("createdBy").get("id"), createdById.get()));
        }
        if (type.isPresent() && !type.get().isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("recordType"), type.get()));
        }
        if (start.isPresent() && end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.between(root.get("createdAt"), start.get(), end.get()));
        } else if (start.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start.get()));
        } else if (end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), end.get()));
        }
        return spec;
    }
}

