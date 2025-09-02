package com.example.emr_server.repository;

import com.example.emr_server.entity.PasswordHistory;
import com.example.emr_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {
    List<PasswordHistory> findTop10ByUserOrderByCreatedAtDesc(User user);
}

