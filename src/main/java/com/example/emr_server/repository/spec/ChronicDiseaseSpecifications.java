package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.ChronicDisease;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class ChronicDiseaseSpecifications {
    private ChronicDiseaseSpecifications() {}

    public static Specification<ChronicDisease> withFilters(
            Optional<UUID> patientId,
            Optional<String> diseaseName,
            Optional<LocalDate> start,
            Optional<LocalDate> end,
            Optional<String> notesFragment
    ) {
        Specification<ChronicDisease> spec = Specification.where(null);
        if (patientId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("patient").get("id"), patientId.get()));
        }
        if (diseaseName.isPresent() && !diseaseName.get().isBlank()) {
            String like = "%" + diseaseName.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("diseaseName")), like));
        }
        if (start.isPresent() && end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.between(root.get("diagnosedDate"), start.get(), end.get()));
        } else if (start.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("diagnosedDate"), start.get()));
        } else if (end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("diagnosedDate"), end.get()));
        }
        if (notesFragment.isPresent() && !notesFragment.get().isBlank()) {
            String like = "%" + notesFragment.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("notes")), like));
        }
        return spec;
    }
}

