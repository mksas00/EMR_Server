package com.example.emr_server.service;

import com.example.emr_server.controller.dto.MedicationDto;
import com.example.emr_server.controller.dto.MedicationPackageDto;
import com.example.emr_server.entity.Medication;
import com.example.emr_server.entity.MedicationPackage;
import com.example.emr_server.repository.MedicationPackageRepository;
import com.example.emr_server.repository.MedicationRepository;
import com.example.emr_server.repository.spec.MedicationSpecifications;
import com.example.emr_server.service.dto.MedicationUrplImportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class MedicationServiceImpl implements MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicationPackageRepository packageRepository;

    public MedicationServiceImpl(MedicationRepository medicationRepository,
                                 MedicationPackageRepository packageRepository) {
        this.medicationRepository = medicationRepository;
        this.packageRepository = packageRepository;
    }

    private MedicationDto toDto(Medication m) {
        return new MedicationDto(
                m.getId(),
                m.getName(),
                m.getCommonName(),
                m.getStrength(),
                m.getPharmaceuticalForm(),
                m.getAuthorizationNumber(),
                m.getAuthorizationValidTo(),
                m.getMarketingAuthorizationHolder(),
                m.getProcedureType(),
                m.getLegalBasis(),
                m.getAtcCode(),
                m.getActiveSubstances(),
                m.getTargetSpecies(),
                m.getPrescriptionCategory()
        );
    }

    private MedicationPackageDto toDto(MedicationPackage p) {
        return new MedicationPackageDto(
                p.getId(),
                p.getMedication()!=null? p.getMedication().getId(): null,
                p.getGtin(),
                p.getPackDescription(),
                p.getSupplyStatus()
        );
    }

    @Override
    public Page<MedicationDto> search(String q, String atc, Pageable pageable) {
        String qNorm = (q==null || q.isBlank())? null : q.trim();
        String atcNorm = (atc==null || atc.isBlank())? null : atc.trim();
        return medicationRepository.findAll(MedicationSpecifications.search(qNorm, atcNorm), pageable)
                .map(this::toDto);
    }

    @Override
    public Optional<MedicationPackageDto> findPackageByGtin(String gtin) {
        if (gtin == null || gtin.isBlank()) return Optional.empty();
        return packageRepository.findByGtin(gtin.trim()).map(this::toDto);
    }

    @Override
    public MedicationDto importUrplRecord(MedicationUrplImportRequest req) {
        // Upsert preferencyjnie po urplId, fallback po numerze pozwolenia
        Medication m = null;
        if (req.urplId() != null) {
            m = medicationRepository.findByUrplId(req.urplId()).orElse(null);
        }
        if (m == null && req.registryNumber() != null && !req.registryNumber().isBlank()) {
            m = medicationRepository.findByAuthorizationNumber(req.registryNumber()).orElse(null);
        }
        if (m == null) m = new Medication();
        if (req.urplId() != null) m.setUrplId(req.urplId());
        m.setName(req.medicinalProductName());
        m.setCommonName(req.commonName());
        m.setStrength(req.medicinalProductPower());
        m.setPharmaceuticalForm(req.pharmaceuticalFormName());
        m.setAuthorizationNumber(req.registryNumber());
        m.setAuthorizationValidTo(parseExpiration(req.expirationDateString()));
        m.setMarketingAuthorizationHolder(req.subjectMedicinalProductName());
        m.setProcedureType(req.procedureTypeName());
        m.setLegalBasis(null); // brak w danych wejściowych
        m.setAtcCode(emptyToNull(req.atcCode()));
        m.setActiveSubstances(req.activeSubstanceName());
        m.setTargetSpecies(emptyToNull(req.targetSpecies()));
        m.setPackagingConsent(emptyToNull(req.packagingConsent()));
        m.setPrescriptionCategory(emptyToNull(req.prescriptionCategory()));
        if (m.getCreatedAt()==null) m.setCreatedAt(Instant.now());
        Medication saved = medicationRepository.save(m);

        if (req.gtin()!=null && !req.gtin().isBlank()) {
            MedicationPackage pack = packageRepository.findByGtin(req.gtin().trim()).orElseGet(MedicationPackage::new);
            pack.setMedication(saved);
            pack.setGtin(req.gtin().trim());
            pack.setPackDescription(req.packDescription());
            if (pack.getCreatedAt()==null) pack.setCreatedAt(Instant.now());
            packageRepository.save(pack);
        }
        return toDto(saved);
    }

    private static LocalDate parseExpiration(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.equalsIgnoreCase("Bezterminowe")) return null;
        try { return LocalDate.parse(v); } catch (DateTimeParseException e) { return null; }
    }

    private static String emptyToNull(String s) { return (s==null || s.isBlank())? null : s; }
}
