package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.LabResult;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class LabResultSpecifications {
    private LabResultSpecifications() {}

    public static Specification<LabResult> withFilters(
            Optional<UUID> patientId,
            Optional<UUID> orderedById,
            Optional<String> status,
            Optional<LocalDate> start,
            Optional<LocalDate> end,
            Optional<String> testName,
            Optional<String> resultFragment
    ) {
        Specification<LabResult> spec = Specification.where(null);
        if (patientId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("patient").get("id"), patientId.get()));
        }
        if (orderedById.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("orderedBy").get("id"), orderedById.get()));
        }
        if (status.isPresent() && !status.get().isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status.get()));
        }
        if (start.isPresent() && end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.between(root.get("resultDate"), start.get(), end.get()));
        } else if (start.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("resultDate"), start.get()));
        } else if (end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("resultDate"), end.get()));
        }
        if (testName.isPresent() && !testName.get().isBlank()) {
            String like = "%" + testName.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("testName")), like));
        }
        if (resultFragment.isPresent() && !resultFragment.get().isBlank()) {
            String like = "%" + resultFragment.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("result")), like));
        }
        return spec;
    }
}

