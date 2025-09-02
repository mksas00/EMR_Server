package com.example.emr_server.security;

import com.example.emr_server.entity.SecurityIncident;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.SecurityIncidentRepository;
import com.example.emr_server.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalSecurityExceptionHandler {

    private final SecurityIncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public GlobalSecurityExceptionHandler(SecurityIncidentRepository incidentRepository, UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String,Object>> handleSecurity(SecurityException ex, HttpServletRequest request) {
        User current = SecurityUtil.getCurrentUser(userRepository).orElse(null);
        SecurityIncident si = new SecurityIncident();
        si.setSeverity("medium");
        si.setCategory("authorization");
        si.setDescription("Access denied: " + ex.getMessage() + " path=" + request.getRequestURI());
        si.setDetectedAt(Instant.now());
        si.setStatus("open");
        si.setUser(current);
        incidentRepository.save(si);
        Map<String,Object> body = new HashMap<>();
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}

