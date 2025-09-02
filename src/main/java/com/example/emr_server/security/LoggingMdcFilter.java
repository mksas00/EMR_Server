package com.example.emr_server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class LoggingMdcFilter extends OncePerRequestFilter {

    public static final String REQ_ID = "reqId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String reqId = UUID.randomUUID().toString();
        try {
            MDC.put(REQ_ID, reqId);
            MDC.put("ip", clientIp(request));
            MDC.put("ua", sanitizeUa(request.getHeader("User-Agent")));
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails cud) {
                var user = cud.getDomainUser();
                if (user != null && user.getId() != null) {
                    MDC.put("userId", user.getId().toString());
                    MDC.put("role", String.valueOf(user.getRole()));
                }
            }
            response.setHeader("X-Request-Id", reqId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return req.getRemoteAddr();
    }

    private static String sanitizeUa(String ua) {
        if (ua == null) return "";
        return ua.length() > 200 ? ua.substring(0, 200) : ua;
    }
}

