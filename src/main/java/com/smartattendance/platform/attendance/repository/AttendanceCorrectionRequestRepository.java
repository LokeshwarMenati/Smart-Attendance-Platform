package com.smartattendance.platform.attendance.repository;

import com.smartattendance.platform.attendance.entity.AttendanceCorrectionRequest;
import com.smartattendance.platform.attendance.entity.CorrectionRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceCorrectionRequestRepository extends JpaRepository<AttendanceCorrectionRequest, Long> {
    @Query("""
        SELECT r FROM AttendanceCorrectionRequest r
        JOIN FETCH r.attendance a
        JOIN FETCH r.requester requester
        LEFT JOIN FETCH r.reviewer reviewer
        WHERE r.publicId = :id
    """)
    Optional<AttendanceCorrectionRequest> findByPublicId(@Param("id") UUID id);

    @Query("""
        SELECT r FROM AttendanceCorrectionRequest r
        JOIN FETCH r.attendance a
        JOIN FETCH r.requester requester
        LEFT JOIN FETCH r.reviewer reviewer
        WHERE (:requesterId IS NULL OR requester.publicId = :requesterId)
          AND (:status IS NULL OR r.status = :status)
        ORDER BY r.createdAt DESC
    """)
    Page<AttendanceCorrectionRequest> findAllForScope(@Param("requesterId") UUID requesterId,
                                                       @Param("status") CorrectionRequestStatus status,
                                                       Pageable pageable);

    boolean existsByAttendanceIdAndStatus(Long attendanceId, CorrectionRequestStatus status);
}
