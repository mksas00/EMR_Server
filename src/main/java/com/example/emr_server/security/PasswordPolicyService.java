package com.example.emr_server.security;

import com.example.emr_server.entity.PasswordHistory;
import com.example.emr_server.entity.User;
import com.example.emr_server.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    // Konfigurowalne parametry (ew. przenieść do properties)
    private static final int MIN_LENGTH = 12;
    private static final int HISTORY_CHECK = 5; // liczba poprzednich haseł do sprawdzenia

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/`~|]");

    private static final Set<String> COMMON = new HashSet<>(List.of(
            "password","passw0rd","p@ssw0rd","123456","123456789","qwerty","admin","letmein","welcome","changeme","zaq12wsx","iloveyou"
    ));

    public List<String> validate(User user, String newPassword) {
        List<String> errors = new ArrayList<>();
        if (newPassword == null || newPassword.isBlank()) {
            errors.add("Hasło puste");
            return errors;
        }
        if (newPassword.length() < MIN_LENGTH) errors.add("Min długość " + MIN_LENGTH);
        if (!UPPER.matcher(newPassword).find()) errors.add("Brak wielkiej litery");
        if (!LOWER.matcher(newPassword).find()) errors.add("Brak małej litery");
        if (!DIGIT.matcher(newPassword).find()) errors.add("Brak cyfry");
        if (!SPECIAL.matcher(newPassword).find()) errors.add("Brak znaku specjalnego");
        String lower = newPassword.toLowerCase();
        if (COMMON.contains(lower)) errors.add("Zbyt popularne hasło");
        if (user.getUsername() != null && lower.contains(user.getUsername().toLowerCase())) errors.add("Hasło zawiera username");
        if (user.getEmail() != null) {
            String local = user.getEmail().split("@")[0].toLowerCase();
            if (lower.contains(local)) errors.add("Hasło zawiera fragment email");
        }
        // Sprawdzenie historii
        List<PasswordHistory> history = passwordHistoryRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        int checked = 0;
        for (PasswordHistory ph : history) {
            if (checked >= HISTORY_CHECK) break;
            if (passwordEncoder.matches(newPassword, ph.getPasswordHash())) {
                errors.add("Hasło użyte wcześniej");
                break;
            }
            checked++;
        }
        // Porównanie z aktualnym (może nie być w historii)
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            errors.add("Nowe hasło identyczne jak obecne");
        }
        return errors;
    }
}

