package com.example.emr_server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for CookieSecurityFilter
 * Tests cookie security wrapper functionality
 */
@ExtendWith(MockitoExtension.class)
class CookieSecurityFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CookieSecurityFilter cookieSecurityFilter;

    @Test
    @DisplayName("doFilterInternal_normalRequest_wrapsResponseWithSecurityWrapper")
    void doFilterInternal_normalRequest_wrapsResponseWithSecurityWrapper() throws ServletException, IOException {
        // Given: normal HTTP request and response
        // When: filter processes request
        cookieSecurityFilter.doFilterInternal(request, response, filterChain);

        // Then: filter chain continues with wrapped response
        verify(filterChain).doFilter(eq(request), any(CookieSecurityResponseWrapper.class));
        verifyNoMoreInteractions(filterChain);
    }

    @Test
    @DisplayName("doFilterInternal_multipleRequests_eachRequestProcessedIndependently")
    void doFilterInternal_multipleRequests_eachRequestProcessedIndependently() throws ServletException, IOException {
        // Given: multiple requests
        // When: processing multiple requests
        cookieSecurityFilter.doFilterInternal(request, response, filterChain);
        cookieSecurityFilter.doFilterInternal(request, response, filterChain);

        // Then: each request processed independently
        verify(filterChain, times(2)).doFilter(eq(request), any(CookieSecurityResponseWrapper.class));
        verifyNoMoreInteractions(filterChain);
    }
}
