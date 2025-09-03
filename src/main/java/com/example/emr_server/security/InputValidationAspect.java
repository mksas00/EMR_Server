package com.example.emr_server.security;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Aspect for automatic input validation on controller methods
 * Addresses OWASP A03:2021 – Injection
 */
@Aspect
@Component
@RequiredArgsConstructor
public class InputValidationAspect {

    private final InputValidationService inputValidationService;
    private final SecurityMonitoringService securityMonitoringService;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object validateControllerInputs(ProceedingJoinPoint joinPoint) throws Throwable {

        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Validate all string arguments
        for (Object arg : args) {
            if (arg != null) {
                validateObject(arg, className + "." + methodName);
            }
        }

        return joinPoint.proceed();
    }

    private void validateObject(Object obj, String context) {
        if (obj instanceof String str) {
            if (!inputValidationService.isSafeInput(str)) {
                securityMonitoringService.logSecurityEvent(
                    SecurityMonitoringService.SecurityEventType.SUSPICIOUS_QUERY_PATTERN,
                    getCurrentUserId(),
                    "Malicious input detected in " + context,
                    getCurrentUserIp()
                );
                throw new SecurityException("Invalid input detected");
            }
        } else {
            // Validate string fields in DTOs
            validateStringFields(obj, context);
        }
    }

    private void validateStringFields(Object obj, String context) {
        if (obj == null) return;

        try {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == String.class) {
                    field.setAccessible(true);
                    String value = (String) field.get(obj);
                    if (value != null && !inputValidationService.isSafeInput(value)) {
                        securityMonitoringService.logSecurityEvent(
                            SecurityMonitoringService.SecurityEventType.SUSPICIOUS_QUERY_PATTERN,
                            getCurrentUserId(),
                            String.format("Malicious input in field %s.%s", context, field.getName()),
                            getCurrentUserIp()
                        );
                        throw new SecurityException("Invalid input detected in field: " + field.getName());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            // Log but don't block - validation best effort
        }
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
