package com.example.emr_server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to ensure all cookies have secure flags set
 * Addresses OWASP A02:2021 – Cryptographic Failures
 */
@Component
public class CookieSecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        // Wrap response to intercept cookie setting
        CookieSecurityResponseWrapper wrappedResponse = new CookieSecurityResponseWrapper(response);

        filterChain.doFilter(request, wrappedResponse);
    }
}
