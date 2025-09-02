package com.example.emr_server.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RlsContextAspect {

    @PersistenceContext
    private EntityManager em;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || within(@org.springframework.transaction.annotation.Transactional *)")
    public Object setRlsContext(ProceedingJoinPoint pjp) throws Throwable {
        boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
        if (txActive) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails cud) {
                var user = cud.getDomainUser();
                try {
                    em.joinTransaction();
                    em.createNativeQuery("SELECT set_config('emr.current_user_id', :uid, true)")
                      .setParameter("uid", user.getId().toString())
                      .getSingleResult();
                    em.createNativeQuery("SELECT set_config('emr.current_user_role', :role, true)")
                      .setParameter("role", user.getRole())
                      .getSingleResult();
                } catch (Exception ignored) {
                    // brak przerwania logiki w razie niedostępności kontekstu
                }
            }
        }
        return pjp.proceed();
    }
}
