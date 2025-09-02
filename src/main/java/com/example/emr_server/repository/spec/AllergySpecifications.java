package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.Allergy;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

public final class AllergySpecifications {
    private AllergySpecifications() {}

    public static Specification<Allergy> withFilters(
            Optional<UUID> patientId,
            Optional<String> allergen,
            Optional<String> severity,
            Optional<UUID> notedById
    ) {
        Specification<Allergy> spec = Specification.where(null);
        if (patientId != null && patientId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("patient").get("id"), patientId.get()));
        }
        if (allergen != null && allergen.isPresent() && !allergen.get().isBlank()) {
            String like = "%" + allergen.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("allergen")), like));
        }
        if (severity != null && severity.isPresent() && !severity.get().isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("severity"), severity.get()));
        }
        if (notedById != null && notedById.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("notedBy").get("id"), notedById.get()));
        }
        return spec;
    }
}

