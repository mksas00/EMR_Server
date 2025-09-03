package com.example.emr_server.security;

import com.example.emr_server.entity.User;
import com.example.emr_server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static Optional<User> getCurrentUser(UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return Optional.ofNullable(cud.getDomainUser());
        }
        // jeśli principal to UUID lub username/email
        String name = auth.getName();
        // spróbuj jako UUID
        try {
            UUID id = UUID.fromString(name);
            return userRepository.findById(id);
        } catch (IllegalArgumentException ignored) {
        }
        return userRepository.findByUsernameOrEmail(name, name);
    }

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return cud.getDomainUser().getId();
        }
        
        // Fallback - try to parse name as UUID
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Cannot determine current user ID", e);
        }
    }

    public static String getCurrentUserIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // Check for real IP behind proxy
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
