package com.example.emr_server.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Response wrapper that automatically adds security flags to all cookies
 * Implements HttpOnly, Secure, and SameSite=Strict for session protection
 */
public class CookieSecurityResponseWrapper extends HttpServletResponseWrapper {

    public CookieSecurityResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void addCookie(Cookie cookie) {
        // Set security flags for all cookies
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Only send over HTTPS

        // Add SameSite=Strict via Set-Cookie header manipulation
        String cookieValue = String.format("%s=%s; Path=%s; HttpOnly; Secure; SameSite=Strict",
                cookie.getName(),
                cookie.getValue() != null ? cookie.getValue() : "",
                cookie.getPath() != null ? cookie.getPath() : "/");

        if (cookie.getMaxAge() >= 0) {
            cookieValue += "; Max-Age=" + cookie.getMaxAge();
        }

        if (cookie.getDomain() != null) {
            cookieValue += "; Domain=" + cookie.getDomain();
        }

        super.addHeader("Set-Cookie", cookieValue);
    }
}
