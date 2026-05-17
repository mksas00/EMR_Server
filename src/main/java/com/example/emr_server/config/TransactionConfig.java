package com.example.emr_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Explicit transaction management configuration.
 * Sets the order to 0 so TransactionInterceptor runs BEFORE (outer) RlsContextAspect.
 * RlsContextAspect has @Order(Ordered.LOWEST_PRECEDENCE) = inner.
 * This ensures GUC variables are set INSIDE an open transaction.
 */
@Configuration
@EnableTransactionManagement(order = 0)
public class TransactionConfig {
}

