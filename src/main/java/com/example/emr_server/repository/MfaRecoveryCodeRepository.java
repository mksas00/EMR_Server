package com.example.emr_server.repository;

import com.example.emr_server.entity.MfaRecoveryCode;
import com.example.emr_server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {
    List<MfaRecoveryCode> findByUserAndUsedAtIsNull(User user);

    @Query("select r from MfaRecoveryCode r where r.user = :user and r.usedAt is null and r.codeHash = :codeHash")
    Optional<MfaRecoveryCode> findActiveByUserAndCodeHash(@Param("user") User user, @Param("codeHash") String codeHash);
}
