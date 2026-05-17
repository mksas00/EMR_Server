package com.example.emr_server.repository;

import com.example.emr_server.entity.User;
import com.example.emr_server.entity.UserLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserLoginAttemptRepository extends JpaRepository<UserLoginAttempt, UUID>, JpaSpecificationExecutor<UserLoginAttempt> {
    List<UserLoginAttempt> findTop20ByUserOrderByTimestampDesc(User user);
    long countByUserAndSuccessIsFalseAndTimestampAfter(User user, Instant after);

    long countBySuccessAndTimestampAfter(Boolean success, Instant after);

    long countByIpAndSuccessFalseAndTimestampAfter(String ip, Instant after);

    @Query("""
            SELECT COUNT(DISTINCT a.user.id)
            FROM UserLoginAttempt a
            WHERE a.ip = :ip AND a.success = false AND a.timestamp >= :since AND a.user IS NOT NULL
            """)
    long countDistinctUsersFailingFromIp(@Param("ip") String ip, @Param("since") Instant since);

    @Query("""
            SELECT a.ip AS ip, COUNT(a) AS cnt
            FROM UserLoginAttempt a
            WHERE a.success = false AND a.timestamp >= :since AND a.ip IS NOT NULL
            GROUP BY a.ip
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> topFailingIpsSince(@Param("since") Instant since,
                                      org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT a.user.username AS username, COUNT(a) AS cnt
            FROM UserLoginAttempt a
            WHERE a.timestamp >= :since AND a.user IS NOT NULL
            GROUP BY a.user.username
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> topActiveUsersSince(@Param("since") Instant since,
                                       org.springframework.data.domain.Pageable pageable);
}
