package com.example.emr_server.security.encryption;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FieldEncryptionService – generyczny serwis szyfrowania pól:
 * Format deterministyczny: v1d:k1:<base64url(iv||ct)>
 * Format losowy:          v1r:k1:<base64url(iv||ct)>
 * Legacy PESEL (poprzedni): v1.<base64url(iv||ct)> – wspierany przy deszyfracji.
 * Uwaga: deterministyczne szyfrowanie ujawnia czy dwie wartości są takie same.
 */
@Slf4j
@Service
public class EncryptionService {

    @Value("${security.enc.master-key:}")
    private String masterKeyB64; // 32B Base64

    @Value("${security.enc.active-key-id:k1}")
    private String activeKeyId;

    private SecretKeySpec masterKey;
    private static EncryptionService INSTANCE;

    private static final SecureRandom RNG = new SecureRandom();
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;

    private final Map<String, SecretKeySpec> deterministicSubKeys = new ConcurrentHashMap<>();
    private final Map<String, SecretKeySpec> randomSubKeys = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        if (masterKeyB64 == null || masterKeyB64.isBlank()) {
            byte[] tmp = new byte[32];
            RNG.nextBytes(tmp);
            log.warn("Brak security.enc.master-key – generowany EFEMERYCZNY (utrata możliwości odszyfrowania po restarcie!)");
            masterKey = new SecretKeySpec(tmp, "AES");
        } else {
            byte[] raw = Base64.getDecoder().decode(masterKeyB64);
            if (raw.length != 32) throw new IllegalStateException("Master key musi mieć 32 bajty");
            masterKey = new SecretKeySpec(raw, "AES");
        }
        INSTANCE = this;
        log.info("EncryptionService zainicjalizowany (keyId={})", activeKeyId);
    }

    public static EncryptionService get() { return INSTANCE; }

    public boolean looksEncrypted(String v) {
        if (v == null) return false;
        return v.startsWith("v1d:") || v.startsWith("v1r:") || v.startsWith("v1.");
    }

    // PUBLIC API (Deterministyczne)
    public String encryptDeterministic(String fieldName, String plaintext) {
        if (plaintext == null) return null;
        if (isAlreadyCiphertext(plaintext)) return plaintext; // idempotent
        try {
            SecretKeySpec key = subKeyDeterministic(fieldName);
            byte[] iv = deriveDeterministicIv(key, plaintext);
            byte[] ct = aesGcm(key, iv, plaintext.getBytes());
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv,0,out,0,iv.length);
            System.arraycopy(ct,0,out,iv.length,ct.length);
            return "v1d:" + activeKeyId + ":" + b64(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encrypt deterministic failed", e);
        }
    }

    public String decryptDeterministic(String fieldName, String ciphertext) {
        if (ciphertext == null) return null;
        if (ciphertext.startsWith("v1d:")) {
            // Oczekiwany format: v1d:<keyId>:<base64>
            int first = ciphertext.indexOf(':', 4); // po prefiksie próbujemy znaleźć kolon po keyId
            if (first < 0) throw new IllegalArgumentException("Niepoprawny format ciphertext (brak separatora po keyId)");
            String keyId = ciphertext.substring(4, first);
            String b64 = ciphertext.substring(first + 1);
            if (b64.isBlank()) throw new IllegalArgumentException("Niepoprawny format ciphertext (brak danych)");
            byte[] all = fromB64(b64);
            if (all.length < IV_LEN + 16) throw new IllegalArgumentException("Ciphertext za krótki");
            byte[] iv = slice(all, 0, IV_LEN);
            byte[] ct = slice(all, IV_LEN, all.length - IV_LEN);
            try {
                // aktualnie ignorujemy keyId przy wyprowadzaniu subkey (rotacja wymaga mapy keyId->master/sub key)
                SecretKeySpec key = subKeyDeterministic(fieldName);
                return new String(aesGcmDecrypt(key, iv, ct));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Decrypt deterministic failed", e);
            }
        }
        if (ciphertext.startsWith("v1.")) { // legacy
            String b64 = ciphertext.substring(3);
            byte[] all = fromB64(b64);
            if (all.length < IV_LEN + 16) return ciphertext; // niepoprawne – pozostaw
            byte[] iv = slice(all, 0, IV_LEN);
            byte[] ct = slice(all, IV_LEN, all.length - IV_LEN);
            try {
                SecretKeySpec key = subKeyDeterministic(fieldName);
                return new String(aesGcmDecrypt(key, iv, ct));
            } catch (Exception e) {
                return ciphertext;
            }
        }
        return ciphertext; // plaintext
    }

    // PUBLIC API (Losowe)
    public String encryptRandom(String fieldName, String plaintext) {
        if (plaintext == null) return null;
        if (isAlreadyCiphertext(plaintext)) return plaintext;
        try {
            SecretKeySpec key = subKeyRandom(fieldName);
            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv);
            byte[] ct = aesGcm(key, iv, plaintext.getBytes());
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv,0,out,0,iv.length);
            System.arraycopy(ct,0,out,iv.length,ct.length);
            return "v1r:" + activeKeyId + ":" + b64(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encrypt random failed", e);
        }
    }

    public String decryptRandom(String fieldName, String ciphertext) {
        if (ciphertext == null) return null;
        if (ciphertext.startsWith("v1r:")) {
            // Format: v1r:<keyId>:<base64>
            int first = ciphertext.indexOf(':', 4);
            if (first < 0) throw new IllegalArgumentException("Niepoprawny format ciphertext random (brak separatora po keyId)");
            String b64 = ciphertext.substring(first + 1);
            if (b64.isBlank()) throw new IllegalArgumentException("Niepoprawny format ciphertext random (brak danych)");
            byte[] all = fromB64(b64);
            if (all.length < IV_LEN + 16) throw new IllegalArgumentException("Ciphertext random za krótki");
            byte[] iv = slice(all, 0, IV_LEN);
            byte[] ct = slice(all, IV_LEN, all.length - IV_LEN);
            try {
                SecretKeySpec key = subKeyRandom(fieldName);
                return new String(aesGcmDecrypt(key, iv, ct));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Decrypt random failed", e);
            }
        }
        return ciphertext; // plaintext
    }

    private SecretKeySpec subKeyDeterministic(String field) {
        return deterministicSubKeys.computeIfAbsent(field, f -> deriveSubKey("DET:"+f));
    }
    private SecretKeySpec subKeyRandom(String field) {
        return randomSubKeys.computeIfAbsent(field, f -> deriveSubKey("RND:"+f));
    }

    private SecretKeySpec deriveSubKey(String info) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(masterKey);
            byte[] out = mac.doFinal(info.getBytes());
            // 32 bajty -> AES-256
            return new SecretKeySpec(out, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Subkey derivation failed", e);
        }
    }

    private byte[] deriveDeterministicIv(SecretKeySpec subKey, String plaintext) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(subKey);
        byte[] full = mac.doFinal(plaintext.getBytes());
        byte[] iv = new byte[IV_LEN];
        System.arraycopy(full,0,iv,0,IV_LEN);
        return iv;
    }

    private byte[] aesGcm(SecretKeySpec key, byte[] iv, byte[] data) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(data);
    }
    private byte[] aesGcmDecrypt(SecretKeySpec key, byte[] iv, byte[] ct) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(ct);
    }

    private boolean isAlreadyCiphertext(String v) {
        return v.startsWith("v1d:") || v.startsWith("v1r:") || v.startsWith("v1.");
    }

    private static String b64(byte[] in) { return Base64.getUrlEncoder().withoutPadding().encodeToString(in); }
    private static byte[] fromB64(String b) { return Base64.getUrlDecoder().decode(b); }
    private static byte[] slice(byte[] a, int off, int len) { byte[] r = new byte[len]; System.arraycopy(a,off,r,0,len); return r; }
}
