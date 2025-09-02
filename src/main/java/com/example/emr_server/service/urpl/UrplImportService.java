package com.example.emr_server.service.urpl;

import com.example.emr_server.service.MedicationService;
import com.example.emr_server.service.dto.MedicationUrplImportRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class UrplImportService {
    private static final Logger log = LoggerFactory.getLogger(UrplImportService.class);

    private final UrplClient urplClient;
    private final MedicationService medicationService;

    public UrplImportService(UrplClient urplClient, MedicationService medicationService) {
        this.urplClient = urplClient;
        this.medicationService = medicationService;
    }

    // Uruchamiane raz dziennie o 02:30
    @Scheduled(cron = "0 30 2 * * *")
    public void scheduledImport() {
        try {
            runImport(0, 100, 5); // defensywnie: 5 stron na noc na start; można zwiększyć po obserwacji
        } catch (Exception e) {
            log.error("URPL scheduled import failed", e);
        }
    }

    public int runImport(int startPage, int size, int maxPages) {
        int imported = 0;
        int page = startPage;
        for (int i = 0; i < maxPages; i++) {
            var resp = urplClient.fetchPage(page, size);
            if (resp == null || resp.getContent() == null || resp.getContent().isEmpty()) {
                break;
            }
            for (var p : resp.getContent()) {
                try {
                    var req = new MedicationUrplImportRequest(
                            p.getId(),
                            p.getMedicinalProductName(),
                            p.getCommonName(),
                            p.getPharmaceuticalFormName(),
                            p.getMedicinalProductPower(),
                            p.getActiveSubstanceName(),
                            p.getSubjectMedicinalProductName(),
                            p.getRegistryNumber(),
                            p.getProcedureTypeName(),
                            p.getExpirationDateString(),
                            p.getAtcCode(),
                            p.getTargetSpecies(),
                            null, // prescriptionCategory brak w JSON
                            null, // packagingConsent brak w JSON
                            null, // gtin brak w liście, inny endpoint
                            null  // packDescription brak w liście
                    );
                    medicationService.importUrplRecord(req);
                    imported++;
                } catch (Exception e) {
                    log.warn("URPL record import failed (id={})", p.getId(), e);
                }
            }
            if (resp.isLast()) break;
            page++;
        }
        log.info("URPL import done: {} records", imported);
        return imported;
    }
}

