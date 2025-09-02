package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.MedicalFile;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class MedicalFileSpecifications {
    private MedicalFileSpecifications() {}

    public static Specification<MedicalFile> withFilters(
            Optional<UUID> patientId,
            Optional<UUID> uploadedById,
            Optional<String> mimeType,
            Optional<Instant> start,
            Optional<Instant> end,
            Optional<String> fileNameFragment
    ) {
        Specification<MedicalFile> spec = Specification.where(null);
        if (patientId.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("patient").get("id"), patientId.get()));
        }
        if (uploadedById.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("uploadedBy").get("id"), uploadedById.get()));
        }
        if (mimeType.isPresent() && !mimeType.get().isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("mimeType"), mimeType.get()));
        }
        if (start.isPresent() && end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.between(root.get("uploadedAt"), start.get(), end.get()));
        } else if (start.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("uploadedAt"), start.get()));
        } else if (end.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("uploadedAt"), end.get()));
        }
        if (fileNameFragment.isPresent() && !fileNameFragment.get().isBlank()) {
            String like = "%" + fileNameFragment.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("fileName")), like));
        }
        return spec;
    }
}

