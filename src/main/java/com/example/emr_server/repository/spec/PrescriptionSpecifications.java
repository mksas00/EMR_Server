package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.Prescription;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class PrescriptionSpecifications {
    private PrescriptionSpecifications() {}

    public static Specification<Prescription> withFilters(
            Optional<UUID> patientId,
            Optional<UUID> doctorId,
            Optional<Boolean> active,
            Optional<LocalDate> start,
            Optional<LocalDate> end
    ) {
        Specification<Prescription> spec = Specification.where(null);
        if (patientId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("patient").get("id"), patientId.get()));
        }
        if (doctorId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("doctor").get("id"), doctorId.get()));
        }
        if (start.isPresent() && end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.between(root.get("issuedDate"), start.get(), end.get()));
        } else if (start.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("issuedDate"), start.get()));
        } else if (end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("issuedDate"), end.get()));
        }
        if (active.isPresent() && active.get()) {
            spec = spec.and((root, q, cb) -> cb.greaterThan(root.get("expirationDate"), java.time.LocalDate.now()));
        }
        return spec;
    }
}

