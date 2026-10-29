package com.smartattendance.platform.attendance.repository;

import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    @Query("SELECT a FROM AttendanceRecord a JOIN FETCH a.employee WHERE a.publicId = :publicId")
    Optional<AttendanceRecord> findByPublicIdWithEmployee(@Param("publicId") UUID publicId);

    Optional<AttendanceRecord> findByEmployeeAndAttendanceDate(Employee employee, LocalDate attendanceDate);

    @Query("SELECT a FROM AttendanceRecord a JOIN FETCH a.employee WHERE a.employee.id = :employeeId AND a.status = :status")
    Optional<AttendanceRecord> findByEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                                                        @Param("status") AttendanceStatus status);

    @Query("""
        SELECT a FROM AttendanceRecord a
        JOIN FETCH a.employee e
        WHERE e.id = :employeeId
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
    """)
    Page<AttendanceRecord> findByEmployeeAndDateRange(@Param("employeeId") Long employeeId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     Pageable pageable);

    @Query("""
        SELECT a FROM AttendanceRecord a
        JOIN FETCH a.employee e
        WHERE (:employeePublicId IS NULL OR e.publicId = :employeePublicId)
          AND (:date IS NULL OR a.attendanceDate = :date)
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
          AND (:status IS NULL OR a.status = :status)
    """)
    Page<AttendanceRecord> findAllWithFilters(@Param("employeePublicId") UUID employeePublicId,
                                             @Param("date") LocalDate date,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate,
                                             @Param("status") AttendanceStatus status,
                                             Pageable pageable);

    @Query("""
        SELECT a FROM AttendanceRecord a
        JOIN FETCH a.employee e
        WHERE (:employeePublicId IS NULL OR e.publicId = :employeePublicId)
          AND (:date IS NULL OR a.attendanceDate = :date)
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
          AND (:status IS NULL OR a.status = :status)
          AND (:search IS NULL OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<AttendanceRecord> findAllWithFiltersAndSearch(@Param("employeePublicId") UUID employeePublicId,
                                                       @Param("date") LocalDate date,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("status") AttendanceStatus status,
                                                       @Param("search") String search,
                                                       Pageable pageable);

    @Query("""
        SELECT COUNT(a) FROM AttendanceRecord a
        WHERE a.attendanceDate = :date
    """)
    long countByAttendanceDate(@Param("date") LocalDate date);

    @Query("""
        SELECT COUNT(a) FROM AttendanceRecord a
        WHERE a.attendanceDate = :date AND a.status = :status
    """)
    long countByAttendanceDateAndStatus(@Param("date") LocalDate date, @Param("status") AttendanceStatus status);

    long countByAttendanceDateAndClockInTimeAfter(LocalDate date, java.time.Instant threshold);

    @Query("""
        SELECT COALESCE(SUM(a.durationMinutes), 0) FROM AttendanceRecord a
        WHERE a.attendanceDate = :date AND a.status = 'COMPLETED'
    """)
    long sumDurationMinutesByDate(@Param("date") LocalDate date);

    @Query("""
        SELECT COALESCE(AVG(a.durationMinutes), 0.0) FROM AttendanceRecord a
        WHERE a.attendanceDate = :date AND a.status = 'COMPLETED'
    """)
    double avgDurationMinutesByDate(@Param("date") LocalDate date);

    @Query("""
        SELECT COUNT(a) FROM AttendanceRecord a
        WHERE (:employeeId IS NULL OR a.employee.id = :employeeId)
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
    """)
    long countInRange(@Param("employeeId") Long employeeId,
                      @Param("startDate") LocalDate startDate,
                      @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COUNT(a) FROM AttendanceRecord a
        WHERE (:employeeId IS NULL OR a.employee.id = :employeeId)
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
          AND a.status = :status
    """)
    long countInRangeByStatus(@Param("employeeId") Long employeeId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("status") AttendanceStatus status);

    @Query("""
        SELECT COALESCE(SUM(a.durationMinutes), 0) FROM AttendanceRecord a
        WHERE (:employeeId IS NULL OR a.employee.id = :employeeId)
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
          AND a.status = 'COMPLETED'
    """)
    long sumDurationInRange(@Param("employeeId") Long employeeId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COALESCE(AVG(a.durationMinutes), 0.0) FROM AttendanceRecord a
        WHERE (:employeeId IS NULL OR a.employee.id = :employeeId)
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
          AND a.status = 'COMPLETED'
    """)
    double avgDurationInRange(@Param("employeeId") Long employeeId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT a FROM AttendanceRecord a
        JOIN FETCH a.employee e
        WHERE (:employeeId IS NULL OR e.id = :employeeId)
          AND (:startDate IS NULL OR a.attendanceDate >= :startDate)
          AND (:endDate IS NULL OR a.attendanceDate <= :endDate)
        ORDER BY a.attendanceDate DESC, a.clockInTime DESC
    """)
    java.util.List<AttendanceRecord> findForExport(@Param("employeeId") Long employeeId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT a FROM AttendanceRecord a JOIN FETCH a.employee e
        WHERE a.attendanceDate BETWEEN :startDate AND :endDate
          AND a.status = 'COMPLETED'
        ORDER BY COALESCE(a.durationMinutes, 0) DESC
    """)
    List<AttendanceRecord> findCompletedBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
}
