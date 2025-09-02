package com.example.emr_server.security;

import com.example.emr_server.entity.User;
import com.example.emr_server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static Optional<User> getCurrentUser(UserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails cud) {
            return Optional.ofNullable(cud.getDomainUser());
        }
        // jeśli principal to UUID lub username/email
        String name = auth.getName();
        // spróbuj jako UUID
        try {
            UUID id = UUID.fromString(name);
            return userRepository.findById(id);
        } catch (IllegalArgumentException ignored) {
        }
        return userRepository.findByUsernameOrEmail(name, name);
    }
}

