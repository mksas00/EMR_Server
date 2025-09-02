package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.Medication;
import org.springframework.data.jpa.domain.Specification;

public final class MedicationSpecifications {
    private MedicationSpecifications() {}

    public static Specification<Medication> search(String q, String atc) {
        Specification<Medication> spec = Specification.where(null);
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("commonName")), like)
            ));
        }
        if (atc != null && !atc.isBlank()) {
            String atcTrim = atc.trim();
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("atcCode"), atcTrim));
        }
        return spec;
    }
}

