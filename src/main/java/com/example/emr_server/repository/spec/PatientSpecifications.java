package com.example.emr_server.repository.spec;

import com.example.emr_server.entity.Patient;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class PatientSpecifications {
    private PatientSpecifications() {}

    public static Specification<Patient> withFilters(
            Optional<String> encFirstName,
            Optional<String> encLastName,
            Optional<String> encPesel,
            Optional<LocalDate> dobStart,
            Optional<LocalDate> dobEnd,
            Optional<String> gender,
            Optional<String> addressFragment,
            Optional<UUID> createdById
    ) {
        Specification<Patient> spec = Specification.where(null);
        if (encFirstName != null && encFirstName.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("firstName"), encFirstName.get()));
        }
        if (encLastName != null && encLastName.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("lastName"), encLastName.get()));
        }
        if (encPesel != null && encPesel.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("pesel"), encPesel.get()));
        }
        if (dobStart != null && dobStart.isPresent() && dobEnd != null && dobEnd.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.between(root.get("dateOfBirth"), dobStart.get(), dobEnd.get()));
        } else if (dobStart != null && dobStart.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("dateOfBirth"), dobStart.get()));
        } else if (dobEnd != null && dobEnd.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("dateOfBirth"), dobEnd.get()));
        }
        if (gender != null && gender.isPresent() && !gender.get().isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("gender"), gender.get()));
        }
        if (addressFragment != null && addressFragment.isPresent() && !addressFragment.get().isBlank()) {
            String like = "%" + addressFragment.get().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("address")), like));
        }
        if (createdById != null && createdById.isPresent()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("createdBy").get("id"), createdById.get()));
        }
        return spec;
    }
}

