package com.example.emr_server.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Security audit aspect for automatic logging of critical operations
 * Addresses OWASP A09:2021 – Security Logging and Monitoring Failures
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditAspect {

    private final SecurityMonitoringService securityMonitoringService;

    @AfterReturning("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public void logAuthorizedAccess(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("Authorized access to {}.{}", className, methodName);

        // Log to security monitoring if it's a sensitive operation
        if (isSensitiveOperation(className, methodName)) {
            securityMonitoringService.logSecurityEvent(
                SecurityMonitoringService.SecurityEventType.DATA_ACCESS_VIOLATION,
                getCurrentUserId(),
                String.format("Accessed %s.%s", className, methodName),
                getCurrentUserIp()
            );
        }
    }

    @AfterThrowing(pointcut = "@annotation(org.springframework.security.access.prepost.PreAuthorize)", throwing = "ex")
    public void logAccessDenied(JoinPoint joinPoint, Exception ex) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.warn("Access denied to {}.{}: {}", className, methodName, ex.getMessage());

        securityMonitoringService.logSecurityEvent(
            SecurityMonitoringService.SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT,
            getCurrentUserId(),
            String.format("Failed access to %s.%s - %s", className, methodName, ex.getMessage()),
            getCurrentUserIp()
        );
    }

    private boolean isSensitiveOperation(String className, String methodName) {
        return className.contains("Patient") ||
               className.contains("MedicalRecord") ||
               className.contains("Prescription") ||
               methodName.contains("delete") ||
               methodName.contains("update");
    }

    private String getCurrentUserId() {
        try {
            return SecurityUtil.getCurrentUserId().toString();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String getCurrentUserIp() {
        try {
            return SecurityUtil.getCurrentUserIp();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
