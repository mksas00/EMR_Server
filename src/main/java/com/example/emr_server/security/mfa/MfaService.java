package com.example.emr_server.security.mfa;

import com.example.emr_server.entity.MfaRecoveryCode;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.MfaRecoveryCodeRepository;
import com.example.emr_server.repository.UserRepository;
import com.example.emr_server.security.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;
    private final MfaRecoveryCodeRepository recoveryCodeRepository;

    @Value("${security.mfa.issuer:EMR}")
    private String issuer;

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"; // RFC4648
    private static final SecureRandom RNG = new SecureRandom();

    public boolean isEnabled(User user) {
        return Boolean.TRUE.equals(user.getMfaEnabled()) && user.getMfaSecret() != null;
    }

    @Transactional
    public MfaStart startSetup(User user) {
        if (isEnabled(user)) throw new IllegalStateException("MFA już włączone");
        String secret = generateSecret(32);
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        userRepository.save(user);
        String uri = buildOtpAuthUri(user.getUsername(), secret);
        return new MfaStart(secret, uri);
    }

    @Transactional
    public MfaConfirmResult confirmSetup(User user, String code) {
        if (user.getMfaSecret() == null) throw new IllegalStateException("Brak rozpoczętej konfiguracji");
        if (!verifyTotp(user.getMfaSecret(), code)) throw new IllegalStateException("Kod TOTP niepoprawny");
        user.setMfaEnabled(true);
        userRepository.save(user);
        List<String> plain = generateRecoveryCodes(user, 8);
        return new MfaConfirmResult(true, plain);
    }

    @Transactional
    public void disable(User user) {
        user.setMfaSecret(null);
        user.setMfaEnabled(false);
        userRepository.save(user);
        recoveryCodeRepository.findByUserAndUsedAtIsNull(user).forEach(rc -> {
            rc.setUsedAt(Instant.now());
            recoveryCodeRepository.save(rc);
        });
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(User user) {
        if (!isEnabled(user)) throw new IllegalStateException("MFA nieaktywne");
        // Invalidate existing unused
        recoveryCodeRepository.findByUserAndUsedAtIsNull(user).forEach(rc -> {
            rc.setUsedAt(Instant.now());
            recoveryCodeRepository.save(rc);
        });
        return generateRecoveryCodes(user, 8);
    }

    public int countActiveRecoveryCodes(User user) {
        return recoveryCodeRepository.findByUserAndUsedAtIsNull(user).size();
    }

    public boolean verifyTotp(String secret, String code) {
        if (secret == null || code == null) return false;
        code = code.trim();
        if (!code.matches("^[0-9]{6}$")) return false;
        long timestep = 30L; // seconds
        long now = System.currentTimeMillis() / 1000L;
        long counter = now / timestep;
        for (long drift = -1; drift <= 1; drift++) {
            if (generateTotp(secret, counter + drift).equals(code)) return true;
        }
        return false;
    }

    @Transactional
    public boolean consumeRecoveryCode(User user, String code) {
        if (code == null || code.isBlank()) return false;
        String hash = HashUtil.sha256Hex(code.trim().toLowerCase(Locale.ROOT));
        Optional<MfaRecoveryCode> opt = recoveryCodeRepository.findActiveByUserAndCodeHash(user, hash);
        if (opt.isPresent()) {
            MfaRecoveryCode rc = opt.get();
            rc.setUsedAt(Instant.now());
            recoveryCodeRepository.save(rc);
            return true;
        }
        return false;
    }

    public String buildOtpAuthUri(String username, String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=6&period=30&algorithm=SHA1",
                urlEncode(issuer), urlEncode(username), secret, urlEncode(issuer));
    }

    private String urlEncode(String v) {
        return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8);
    }

    private List<String> generateRecoveryCodes(User user, int count) {
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String c = randomRecoveryCode();
            codes.add(c);
            MfaRecoveryCode rc = new MfaRecoveryCode();
            rc.setId(UUID.randomUUID());
            rc.setUser(user);
            rc.setCodeHash(HashUtil.sha256Hex(c.toLowerCase(Locale.ROOT)));
            recoveryCodeRepository.save(rc);
        }
        return codes;
    }

    private String randomRecoveryCode() {
        // 10 znaków alfanum.
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bez mylących 0/O/1/I
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(RNG.nextInt(chars.length())));
        return sb.toString();
    }

    private String generateSecret(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(BASE32_ALPHABET.charAt(RNG.nextInt(BASE32_ALPHABET.length())));
        return sb.toString();
    }

    private String generateTotp(String base32Secret, long counter) {
        byte[] key = base32Decode(base32Secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (counter & 0xFF);
            counter >>= 8;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hmac = mac.doFinal(data);
            int offset = hmac[hmac.length - 1] & 0xF;
            int binCode = ((hmac[offset] & 0x7F) << 24) |
                    ((hmac[offset + 1] & 0xFF) << 16) |
                    ((hmac[offset + 2] & 0xFF) << 8) |
                    (hmac[offset + 3] & 0xFF);
            int otp = binCode % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP generation failed", e);
        }
    }

    private byte[] base32Decode(String s) {
        s = s.replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0; int bitsLeft = 0; List<Byte> out = new ArrayList<>();
        for (char c : s.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.add((byte)((buffer >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }
        byte[] arr = new byte[out.size()];
        for (int i=0;i<out.size();i++) arr[i]=out.get(i);
        return arr;
    }

    // DTO wewnętrzne
    public record MfaStart(String secret, String otpauthUri) {}
    public record MfaConfirmResult(boolean enabled, List<String> recoveryCodes) {}
}
