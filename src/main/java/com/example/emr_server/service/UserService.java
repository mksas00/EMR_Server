package com.example.emr_server.service;

import com.example.emr_server.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Optional<User> getUserById(UUID id);
    Optional<User> getUserByEmail(String email);
    Optional<User> getUserByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    User saveUser(User user);
    void deleteUser(UUID id);
}