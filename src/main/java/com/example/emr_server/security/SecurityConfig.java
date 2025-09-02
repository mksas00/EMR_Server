package com.example.emr_server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import com.example.emr_server.security.ratelimit.RateLimitFilter; // dodane
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final RateLimitFilter rateLimitFilter; // wstrzyknięcie

    @Value("${app.security.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public RequestSizeLimitFilter requestSizeLimitFilter(@Value("${app.security.max-json-bytes:1048576}") long maxBytes) {
        return new RequestSizeLimitFilter(maxBytes);
    }

    @Bean
    public LoggingMdcFilter loggingMdcFilter() {
        return new LoggingMdcFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RequestSizeLimitFilter requestSizeLimitFilter) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health", "/actuator/info", "/error",
                    "/auth/login", "/auth/refresh", "/auth/logout",
                    "/auth/password-reset/request", "/auth/password-reset/confirm",
                    "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(daoAuthProvider(userDetailsService, passwordEncoder()))
            // Rejestracja filtra MDC tuż przed autoryzacją
            .addFilterBefore(loggingMdcFilter(), AuthorizationFilter.class)
            // Odrzucaj duże JSON-y możliwie wcześnie
            .addFilterBefore(requestSizeLimitFilter, AuthorizationFilter.class)
            // Rate limiting (również przed autoryzacją)
            .addFilterBefore(rateLimitFilter, AuthorizationFilter.class)
            // JWT autoryzacja
            .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
            .headers(h -> {
                // Content Security Policy - ochrona przed XSS
                h.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self'; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'; " +
                    "object-src 'none';"
                ));

                // Podstawowe nagłówki bezpieczeństwa
                h.frameOptions(f -> f.deny()); // Ochrona przed clickjacking
                h.contentTypeOptions(co -> {}); // Ochrona przed MIME sniffing
                h.cacheControl(cc -> {}); // Kontrola cache
                h.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));

                // HTTPS enforcement
                h.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .preload(true)
                    .maxAgeInSeconds(31536000)); // 1 rok

                // Cross-Origin Policies dla izolacji
                h.crossOriginEmbedderPolicy(coep -> coep
                    .policy(org.springframework.security.web.header.writers.CrossOriginEmbedderPolicyHeaderWriter.CrossOriginEmbedderPolicy.REQUIRE_CORP));
                h.crossOriginOpenerPolicy(coop -> coop
                    .policy(org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN));
                h.crossOriginResourcePolicy(corp -> corp
                    .policy(org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN));

                // Niestandardowe nagłówki medyczne EMR
                h.addHeaderWriter((request, response) -> {
                    response.addHeader("X-Medical-Data-Protection", "HIPAA-Compliant");
                    response.addHeader("X-Audit-Required", "true");
                    response.addHeader("X-BTG-Monitoring", "enabled");

                    // Zapobieganie cachowaniu wrażliwych endpointów
                    String requestUri = request.getRequestURI();
                    if (requestUri.contains("/api/patients") ||
                        requestUri.contains("/api/medical") ||
                        requestUri.contains("/api/btg")) {
                        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
                        response.addHeader("Pragma", "no-cache");
                        response.addHeader("Expires", "0");
                    }
                });

                // Permissions Policy
                h.addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()"));
            });
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthProvider(UserDetailsService uds, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
