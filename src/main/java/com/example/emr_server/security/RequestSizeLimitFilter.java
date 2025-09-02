package com.example.emr_server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Prosty filtr ograniczający rozmiar żądań dla JSON (na podstawie nagłówka Content-Length).
 * Jeśli Content-Length przekracza limit, zwraca 413 Payload Too Large bez czytania ciała.
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;
    private final Set<String> methodsToCheck = Set.of("POST", "PUT", "PATCH");

    public RequestSizeLimitFilter(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (shouldCheck(request)) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > 0 && contentLength > maxBytes) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request body too large");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldCheck(HttpServletRequest request) {
        String method = request.getMethod();
        if (!methodsToCheck.contains(method)) return false;
        String contentType = request.getContentType();
        if (contentType == null) return false;
        try {
            MediaType mt = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.includes(mt) ||
                   (mt.getType().equals("application") && mt.getSubtype().endsWith("+json"));
        } catch (Exception ignored) {
            return false;
        }
    }
}

