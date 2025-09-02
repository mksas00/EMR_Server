package com.example.emr_server.repository;

import com.example.emr_server.entity.VisitSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface VisitSlotRepository extends JpaRepository<VisitSlot, UUID> {

    List<VisitSlot> findByDoctor_Id(UUID doctorId);

    @Query("select s from VisitSlot s where s.doctor.id = :doctorId and s.startTime < :end and s.endTime > :start")
    List<VisitSlot> findOverlapping(@Param("doctorId") UUID doctorId,
                                    @Param("start") Instant start,
                                    @Param("end") Instant end);

    @Query("select s from VisitSlot s where (:doctorId is null or s.doctor.id = :doctorId) and (:status is null or s.status = :status) and (:start is null or s.startTime >= :start) and (:end is null or s.endTime <= :end)")
    List<VisitSlot> search(@Param("doctorId") UUID doctorId,
                           @Param("start") Instant start,
                           @Param("end") Instant end,
                           @Param("status") VisitSlot.Status status);
}

