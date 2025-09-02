package com.example.emr_server.security;

import com.example.emr_server.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getDomainUser() { return user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Prosta mapa roli -> authority (ROLE_<ROLE>)
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // brak logiki wygasania konta teraz
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountLocked() == null || !user.getAccountLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // można dodać politykę czasu ważności hasła
    }

    @Override
    public boolean isEnabled() {
        return true; // ewentualnie dodać pole enabled
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserDetails that)) return false;
        return Objects.equals(user.getId(), that.user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(user.getId());
    }
}

