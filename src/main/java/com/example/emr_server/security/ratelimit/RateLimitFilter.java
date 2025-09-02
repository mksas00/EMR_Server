package com.example.emr_server.security.ratelimit;

import com.example.emr_server.entity.SecurityIncident;
import com.example.emr_server.repository.SecurityIncidentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final SecurityIncidentRepository incidentRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // Whitelist minimalny
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.equals("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);

        // Global limit per IP
        RateLimitService.RateCheck global = rateLimitService.tryConsume("GLOBAL_IP", ip);
        if (!global.allowed()) {
            reject(response, ip, path, "GLOBAL_IP", global.resetEpochSeconds());
            return;
        }

        boolean isLogin = path.equals("/auth/login") && "POST".equalsIgnoreCase(request.getMethod());
        boolean isPwdResetRequest = path.equals("/auth/password-reset/request") && "POST".equalsIgnoreCase(request.getMethod());
        boolean isPwdResetConfirm = path.equals("/auth/password-reset/confirm") && "POST".equalsIgnoreCase(request.getMethod());

        if (isLogin || isPwdResetRequest) {
            // Per IP bucket (login albo reset request)
            String ipBucket = isLogin ? "LOGIN_IP" : "PWD_RESET_IP";
            RateLimitService.RateCheck ipCheck = rateLimitService.tryConsume(ipBucket, ip);
            if (!ipCheck.allowed()) {
                reject(response, ip, path, ipBucket, ipCheck.resetEpochSeconds());
                return;
            }
            String contentType = request.getContentType();
            if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
                byte[] bodyBytes = request.getInputStream().readAllBytes();
                String principal = extractLoginPrincipal(bodyBytes); // ten sam parser szuka pola usernameOrEmail
                if (principal != null && !principal.isBlank()) {
                    principal = principal.toLowerCase(Locale.ROOT);
                    String userBucket = isLogin ? "LOGIN_USER" : "PWD_RESET_USER";
                    RateLimitService.RateCheck userCheck = rateLimitService.tryConsume(userBucket, principal);
                    if (!userCheck.allowed()) {
                        reject(response, ip, path, userBucket, userCheck.resetEpochSeconds());
                        return;
                    }
                }
                HttpServletRequest wrapped = new CachedBodyRequest(request, bodyBytes);
                filterChain.doFilter(wrapped, response);
                return;
            }
        } else if (isPwdResetConfirm) {
            RateLimitService.RateCheck conf = rateLimitService.tryConsume("PWD_RESET_CONFIRM_IP", ip);
            if (!conf.allowed()) {
                reject(response, ip, path, "PWD_RESET_CONFIRM_IP", conf.resetEpochSeconds());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String ip, String path, String bucket, long resetEpoch) throws IOException {
        log.warn("RATE_LIMIT_EXCEEDED ip={} path={} bucket={} resetEpoch={}", ip, path, bucket, resetEpoch);
        try {
            SecurityIncident si = new SecurityIncident();
            si.setId(UUID.randomUUID());
            si.setDetectedAt(Instant.now());
            si.setSeverity("medium");
            si.setCategory("RATE_LIMIT");
            si.setDescription("Przekroczono limit: " + bucket + " na " + path + " z IP " + ip);
            si.setStatus("open");
            incidentRepository.save(si);
        } catch (Exception e) {
            log.debug("Nie udało się zapisać incydentu rate limiting: {}", e.getMessage());
        }
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        long retryAfter = Math.max(0, resetEpoch - (System.currentTimeMillis()/1000L));
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        String body = '{' + "\"error\":\"too_many_requests\",\"bucket\":\"" + bucket + "\",\"retryAfterSeconds\":" + retryAfter + '}';
        response.getWriter().write(body);
    }

    private String extractClientIp(HttpServletRequest request) {
        String hdr = request.getHeader("X-Forwarded-For");
        if (hdr != null && !hdr.isBlank()) {
            return hdr.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
        return ip;
    }

    private String extractLoginPrincipal(byte[] body) {
        if (body == null || body.length == 0) return null;
        String json = new String(body, StandardCharsets.UTF_8);
        // Bardzo prosty parsing (unikamy zależności) – case insensitive klucza.
        int idx = json.toLowerCase(Locale.ROOT).indexOf("usernameoremail");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon);
        if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote).trim();
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBodyRequest(HttpServletRequest original, byte[] body) { super(original); this.body = body == null ? new byte[0] : body; }
        @Override public ServletInputStream getInputStream() { return new ServletInputStream() {
            private final ByteArrayInputStream bais = new ByteArrayInputStream(body);
            @Override public int read() { return bais.read(); }
            @Override public boolean isFinished() { return bais.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) { }
        }; }
    }
}
