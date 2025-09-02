package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.Visit;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class VisitSpecifications {
    private VisitSpecifications() {}

    public static Specification<Visit> withFilters(
            Optional<UUID> patientId,
            Optional<UUID> doctorId,
            Optional<Instant> start,
            Optional<Instant> end,
            Optional<String> type,
            Optional<String> diagnosis,
            Optional<String> reason,
            Optional<Boolean> confidential,
            Optional<String> status
    ) {
        Specification<Visit> spec = Specification.where(null);
        if (patientId != null && patientId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("patient").get("id"), patientId.get()));
        }
        if (doctorId != null && doctorId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("doctor").get("id"), doctorId.get()));
        }
        if (start != null && end != null && start.isPresent() && end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.between(root.get("visitDate"), start.get(), end.get()));
        } else if (start != null && start.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("visitDate"), start.get()));
        } else if (end != null && end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("visitDate"), end.get()));
        }
        if (type != null && type.isPresent() && !type.get().isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("visitType"), type.get()));
        }
        if (diagnosis != null && diagnosis.isPresent() && !diagnosis.get().isBlank()) {
            String like = "%" + diagnosis.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("diagnosis")), like));
        }
        if (reason != null && reason.isPresent() && !reason.get().isBlank()) {
            String like = "%" + reason.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("reason")), like));
        }
        if (confidential != null && confidential.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isConfidential"), confidential.get()));
        }
        if (status != null && status.isPresent() && !status.get().isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("status")), status.get().toLowerCase()));
        }
        return spec;
    }
}
