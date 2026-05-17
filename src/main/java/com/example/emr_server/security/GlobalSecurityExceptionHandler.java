package com.example.emr_server.security;

import com.example.emr_server.entity.User;
import com.example.emr_server.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalSecurityExceptionHandler {

    private final SecurityIncidentService incidents;
    private final UserRepository userRepository;

    public GlobalSecurityExceptionHandler(SecurityIncidentService incidents, UserRepository userRepository) {
        this.incidents = incidents;
        this.userRepository = userRepository;
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String,Object>> handleSecurity(SecurityException ex, HttpServletRequest request) {
        User current = SecurityUtil.getCurrentUser(userRepository).orElse(null);
        String path = request.getRequestURI();
        boolean phi = path != null && (
                path.startsWith("/api/patients")
                        || path.startsWith("/api/medical-records")
                        || path.startsWith("/api/lab")
                        || path.startsWith("/api/prescriptions")
                        || path.startsWith("/api/files"));
        String severity = phi ? "high" : "medium";
        String category = phi ? SecurityIncidentService.Cat.UNAUTHORIZED_PHI_ACCESS
                              : SecurityIncidentService.Cat.AUTHORIZATION;
        incidents.raise(severity, category,
                "Access denied: " + ex.getMessage() + " path=" + path, current);

        Map<String,Object> body = new HashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        body.put("path", path);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}

