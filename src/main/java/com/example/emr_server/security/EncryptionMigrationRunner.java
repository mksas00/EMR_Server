package com.example.emr_server.security;

import com.example.emr_server.security.encryption.Encrypted;
import com.example.emr_server.security.encryption.EncryptionService;
import jakarta.persistence.Entity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Batch re-encrypt istniejących rekordów zawierających plaintext w polach oznaczonych @Encrypted.
 * Włączenie: security.encryption.migrate=true
 * Walidacja bez migracji: security.encryption.validate=true (migrate=false)
 * Po udanym migrze – wyłączyć oba property aby uniknąć narzutu przy starcie.
 */
@Slf4j
@Component
@Order(150) // po Flyway i inicjalizacji EncryptionService
public class EncryptionMigrationRunner implements CommandLineRunner {

    @Value("${security.encryption.migrate:false}")
    private boolean migrate;

    @Value("${security.encryption.validate:false}")
    private boolean validateOnly;

    private final List<EntityBundle<?>> bundles;
    private final EncryptionService encryptionService;

    public EncryptionMigrationRunner(
            // Repositories wstrzyknięte przez Spring (dodaj tylko te które mają @Encrypted pola)
            com.example.emr_server.repository.PatientRepository patientRepo,
            com.example.emr_server.repository.AllergyRepository allergyRepo,
            com.example.emr_server.repository.ChronicDiseaseRepository chronicRepo,
            com.example.emr_server.repository.LabResultRepository labRepo,
            com.example.emr_server.repository.MedicationHistoryRepository medHistRepo,
            com.example.emr_server.repository.PrescriptionRepository prescriptionRepo,
            com.example.emr_server.repository.VisitRepository visitRepo,
            com.example.emr_server.repository.UserRepository userRepo,
            EncryptionService encryptionService
    ) {
        this.encryptionService = encryptionService;
        // Kolekcja encji do migracji – kolejność nie krytyczna (brak zależności szyfrowania)
        this.bundles = List.of(
                new EntityBundle<>("Patient", patientRepo, com.example.emr_server.entity.Patient.class),
                new EntityBundle<>("Allergy", allergyRepo, com.example.emr_server.entity.Allergy.class),
                new EntityBundle<>("ChronicDisease", chronicRepo, com.example.emr_server.entity.ChronicDisease.class),
                new EntityBundle<>("LabResult", labRepo, com.example.emr_server.entity.LabResult.class),
                new EntityBundle<>("MedicationHistory", medHistRepo, com.example.emr_server.entity.MedicationHistory.class),
                new EntityBundle<>("Prescription", prescriptionRepo, com.example.emr_server.entity.Prescription.class),
                new EntityBundle<>("Visit", visitRepo, com.example.emr_server.entity.Visit.class),
                new EntityBundle<>("User", userRepo, com.example.emr_server.entity.User.class) // dla mfaSecret
        );
    }

    @Override
    public void run(String... args) {
        if (!migrate && !validateOnly) {
            return; // nic do zrobienia
        }
        log.info("[ENC-MIGRATION] Start (migrate={}, validateOnly={})", migrate, validateOnly);
        long globalToProcess = 0, globalMigrated = 0, globalStillPlain = 0;
        long startNs = System.nanoTime();
        for (EntityBundle<?> b : bundles) {
            MigrationStats stats = processBundle(b);
            globalToProcess += stats.total;
            globalMigrated += stats.migrated;
            globalStillPlain += stats.remainingPlain;
            log.info("[ENC-MIGRATION] {}: scanned={}, migrated={}, stillPlain={}", b.name, stats.total, stats.migrated, stats.remainingPlain);
        }
        double seconds = (System.nanoTime() - startNs)/1_000_000_000d;
        log.info("[ENC-MIGRATION] SUMMARY scanned={}, migrated={}, remainingPlain={}, time={}s", globalToProcess, globalMigrated, globalStillPlain, String.format(Locale.ROOT, "%.2f", seconds));
        if (migrate && globalStillPlain == 0) {
            log.info("[ENC-MIGRATION] Wszystkie pola wyglądają na zaszyfrowane. Wyłącz security.encryption.migrate oraz security.encryption.validate.");
        }
        if (globalStillPlain > 0) {
            log.warn("[ENC-MIGRATION] Pozostały plaintexty ({}). Uruchom ponownie migrację lub sprawdź niestandardowe pola.");
        }
    }

    private <T> MigrationStats processBundle(EntityBundle<T> bundle) {
        Class<T> type = bundle.type;
        List<Field> encryptedFields = Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Encrypted.class))
                .toList();
        if (encryptedFields.isEmpty()) return new MigrationStats();
        encryptedFields.forEach(f -> f.setAccessible(true));
        JpaRepository<T, ?> repo = bundle.repo;
        final int PAGE = 200;
        int page = 0;
        AtomicLong scanned = new AtomicLong();
        AtomicLong migrated = new AtomicLong();
        AtomicLong remainingPlain = new AtomicLong();
        Page<T> p;
        do {
            p = repo.findAll(PageRequest.of(page, PAGE));
            if (p.isEmpty()) break;
            handlePage(encryptedFields, repo, p.getContent(), scanned, migrated, remainingPlain);
            page++;
        } while (p.hasNext());
        return new MigrationStats(scanned.get(), migrated.get(), remainingPlain.get());
    }

    @Transactional
    protected <T> void handlePage(List<Field> encryptedFields, JpaRepository<T, ?> repo, List<T> content,
                                  AtomicLong scanned, AtomicLong migrated, AtomicLong remainingPlain) {
        for (T entity : content) {
            scanned.incrementAndGet();
            boolean changed = false;
            boolean foundPlain = false;
            for (Field f : encryptedFields) {
                try {
                    Object v = f.get(entity);
                    if (v instanceof String s && s != null && !s.isBlank()) {
                        if (!encryptionService.looksEncrypted(s)) {
                            foundPlain = true;
                            if (migrate) {
                                // Szyfruj bezpośrednio – odwzorowanie logiki listenera
                                Encrypted enc = f.getAnnotation(Encrypted.class);
                                String logical = enc.value().isBlank() ? (entity.getClass().getSimpleName().toLowerCase()+"."+f.getName()) : enc.value();
                                String ciphertext = (enc.mode() == Encrypted.Mode.DETERMINISTIC)
                                        ? encryptionService.encryptDeterministic(logical, s.trim())
                                        : encryptionService.encryptRandom(logical, s.trim());
                                f.set(entity, ciphertext);
                                changed = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[ENC-MIGRATION] Błąd szyfrowania {}.{}: {}", entity.getClass().getSimpleName(), f.getName(), e.getMessage());
                }
            }
            if (changed) {
                repo.save(entity); // zapis zaszyfrowanych wartości
                migrated.incrementAndGet();
            } else if (foundPlain) {
                remainingPlain.incrementAndGet();
            }
        }
    }

    private record EntityBundle<T>(String name, JpaRepository<T, ?> repo, Class<T> type) {}
    private record MigrationStats(long total, long migrated, long remainingPlain) { MigrationStats(){this(0,0,0);} }
}
