package com.example.emr_server.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prosty fixed-window rate limiting w pamięci (Caffeine) – wystarczające dla pracy.
 * W opisie można dodać możliwość migracji do Redis (INCR + EXPIRE) dla skalowania.
 */
@Service
public class RateLimitService {
    public record RateLimitSpec(int limit, Duration window) {}
    public record RateCheck(boolean allowed, int remaining, long resetEpochSeconds) {}

    private static class Counter {
        long windowStart; // numer okna
        int count;
    }

    private final Map<String, RateLimitSpec> specs = new ConcurrentHashMap<>();
    private final Cache<String, Counter> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10)) // wystarcza aby stale czyszczone
            .maximumSize(100_000)
            .build();

    public RateLimitService() {
        // Domyślne specy
        specs.put("LOGIN_IP", new RateLimitSpec(20, Duration.ofMinutes(1)));
        specs.put("LOGIN_USER", new RateLimitSpec(5, Duration.ofMinutes(1)));
        specs.put("GLOBAL_IP", new RateLimitSpec(100, Duration.ofMinutes(1)));
        specs.put("HEAVY_IP", new RateLimitSpec(10, Duration.ofMinutes(5)));
        // Reset hasła
        specs.put("PWD_RESET_IP", new RateLimitSpec(5, Duration.ofMinutes(15)));
        specs.put("PWD_RESET_USER", new RateLimitSpec(3, Duration.ofMinutes(15)));
        specs.put("PWD_RESET_CONFIRM_IP", new RateLimitSpec(10, Duration.ofMinutes(10)));
    }

    public RateCheck tryConsume(String bucket, String key) {
        RateLimitSpec spec = specs.get(bucket);
        if (spec == null) {
            return new RateCheck(true, Integer.MAX_VALUE, Instant.now().getEpochSecond()+60);
        }
        long nowMs = System.currentTimeMillis();
        long windowIndex = nowMs / spec.window().toMillis();
        String composite = bucket + ":" + key + ":" + windowIndex;
        Counter c = cache.getIfPresent(composite);
        if (c == null) {
            c = new Counter();
            c.windowStart = windowIndex;
            c.count = 1;
            cache.put(composite, c);
            return new RateCheck(true, spec.limit()-1, windowResetEpoch(spec, windowIndex));
        }
        if (c.count >= spec.limit()) {
            return new RateCheck(false, 0, windowResetEpoch(spec, windowIndex));
        }
        c.count++;
        return new RateCheck(true, spec.limit()-c.count, windowResetEpoch(spec, windowIndex));
    }

    private long windowResetEpoch(RateLimitSpec spec, long windowIndex) {
        long windowStartMs = windowIndex * spec.window().toMillis();
        return (windowStartMs + spec.window().toMillis()) / 1000L; // sekundy UNIX
    }
}
