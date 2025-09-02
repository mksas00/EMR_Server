package com.example.emr_server.service;

import com.example.emr_server.controller.dto.MedicationDto;
import com.example.emr_server.controller.dto.MedicationPackageDto;
import com.example.emr_server.service.dto.MedicationUrplImportRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MedicationService {
    Page<MedicationDto> search(String q, String atc, Pageable pageable);
    Optional<MedicationPackageDto> findPackageByGtin(String gtin);
    MedicationDto importUrplRecord(MedicationUrplImportRequest req);
}

