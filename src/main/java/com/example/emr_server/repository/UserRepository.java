package com.example.emr_server.repository;

import com.example.emr_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Pobiera użytkownika na podstawie adresu e-mail.
     *
     * @param email adres e-mail użytkownika
     * @return obiekt użytkownika, jeśli istnieje
     */
    Optional<User> findByEmail(String email);

    /**
     * Pobiera użytkownika na podstawie nazwy użytkownika.
     *
     * @param username nazwa użytkownika
     * @return obiekt użytkownika, jeśli istnieje
     */
    Optional<User> findByUsername(String username);

    /**
     * Sprawdza, czy użytkownik istnieje na podstawie adresu e-mail.
     *
     * @param email adres e-mail użytkownika
     * @return wartość true, jeśli użytkownik istnieje, inaczej false
     */
    boolean existsByEmail(String email);

    /**
     * Sprawdza, czy użytkownik istnieje na podstawie nazwy użytkownika.
     *
     * @param username nazwa użytkownika
     * @return wartość true, jeśli użytkownik istnieje, inaczej false
     */
    boolean existsByUsername(String username);

    /**
     * Pobiera użytkownika na podstawie nazwy użytkownika lub adresu e-mail.
     *
     * @param username nazwa użytkownika
     * @param email adres e-mail użytkownika
     * @return obiekt użytkownika, jeśli istnieje
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
}