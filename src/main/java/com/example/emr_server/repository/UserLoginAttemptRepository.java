package com.example.emr_server.repository;

import com.example.emr_server.entity.User;
import com.example.emr_server.entity.UserLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserLoginAttemptRepository extends JpaRepository<UserLoginAttempt, UUID> {
    List<UserLoginAttempt> findTop20ByUserOrderByTimestampDesc(User user);
    long countByUserAndSuccessIsFalseAndTimestampAfter(User user, Instant after);
}

