package com.example.emr_server.security.encryption;

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

@Slf4j
public class EncryptionEntityListener {

    @PrePersist
    @PreUpdate
    private void encrypt(Object entity) {
        process(entity, true);
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void decrypt(Object entity) {
        process(entity, false);
    }

    private void process(Object entity, boolean toCipher) {
        EncryptionService svc = EncryptionService.get();
        if (svc == null) return; // jeszcze nie zainicjalizowany
        Class<?> cls = entity.getClass();
        for (Field f : cls.getDeclaredFields()) {
            Encrypted enc = f.getAnnotation(Encrypted.class);
            if (enc == null) continue;
            if (!f.getType().equals(String.class)) continue; // tylko String
            f.setAccessible(true);
            try {
                String current = (String) f.get(entity);
                if (current == null || current.isBlank()) continue;
                String logical = enc.value().isBlank() ? (cls.getSimpleName().toLowerCase()+"."+f.getName()) : enc.value();
                if (toCipher) {
                    // unikamy podwójnego szyfrowania
                    if (svc.looksEncrypted(current)) continue;
                    String ct = (enc.mode() == Encrypted.Mode.DETERMINISTIC)
                            ? svc.encryptDeterministic(logical, current.trim())
                            : svc.encryptRandom(logical, current.trim());
                    f.set(entity, ct);
                } else {
                    // deszyfrowanie – jeśli ciphertext
                    if (!svc.looksEncrypted(current)) continue; // już plaintext
                    String pt;
                    if (current.startsWith("v1d:")) {
                        pt = svc.decryptDeterministic(logical, current);
                    } else if (current.startsWith("v1r:")) {
                        pt = svc.decryptRandom(logical, current);
                    } else if (current.startsWith("v1.")) { // legacy deterministic
                        pt = svc.decryptDeterministic(logical, current);
                    } else {
                        continue;
                    }
                    f.set(entity, pt);
                }
            } catch (Exception e) {
                log.error("Błąd {} pola szyfrowania dla {}.{}: {}", toCipher?"podczas szyfrowania":"deszyfrowania", cls.getSimpleName(), f.getName(), e.getMessage());
            }
        }
    }
}

