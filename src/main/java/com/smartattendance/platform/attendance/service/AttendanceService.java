package com.smartattendance.platform.attendance.service;

import com.smartattendance.platform.attendance.dto.AttendanceResponse;
import com.smartattendance.platform.attendance.dto.ClockInRequest;
import com.smartattendance.platform.attendance.dto.ClockOutRequest;
import com.smartattendance.platform.attendance.dto.AttendanceCorrectionRequest;
import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.audit.event.AuditEvent;
import com.smartattendance.platform.common.exception.DuplicateResourceException;
import com.smartattendance.platform.common.exception.InvalidOperationException;
import com.smartattendance.platform.common.exception.ResourceNotFoundException;
import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.common.util.BusinessTimeService;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.model.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final BusinessTimeService businessTimeService;
    private final ApplicationEventPublisher eventPublisher;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             BusinessTimeService businessTimeService,
                             ApplicationEventPublisher eventPublisher) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.businessTimeService = businessTimeService;
        this.eventPublisher = eventPublisher;
    }

    private Employee getEmployee(UUID publicId) {
        return employeeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID '" + publicId + "' was not found."));
    }

    @Transactional
    public AttendanceResponse clockIn(UserPrincipal principal, ClockInRequest request) {
        Employee employee = getEmployee(principal.getPublicId());

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new AccessDeniedException("Inactive employee account cannot clock in.");
        }

        Instant now = businessTimeService.getCurrentInstant();
        LocalDate businessDate = businessTimeService.getBusinessDate(now);

        // Pre-check for clear business error message
        if (attendanceRepository.findByEmployeeAndAttendanceDate(employee, businessDate).isPresent()) {
            throw new DuplicateResourceException(
                    "Employee " + employee.getEmployeeCode() + " has already clocked in for date " + businessDate + "."
            );
        }

        // Also verify no other open session exists
        if (attendanceRepository.findByEmployeeIdAndStatus(employee.getId(), AttendanceStatus.OPEN).isPresent()) {
            throw new InvalidOperationException("You currently have an uncompleted open attendance session.");
        }

        AttendanceRecord record = new AttendanceRecord(
                UUID.randomUUID(),
                employee,
                businessDate,
                now,
                request != null ? request.notes() : null
        );

        try {
            AttendanceRecord saved = attendanceRepository.saveAndFlush(record);
            log.info("Clock-in recorded for employee {} on date {}", employee.getEmployeeCode(), businessDate);

            eventPublisher.publishEvent(new AuditEvent(
                    employee.getPublicId(), employee.getEmail(), "CLOCK_IN", "ATTENDANCE", saved.getPublicId().toString(), "Date: " + businessDate
            ));

            return AttendanceResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate clock-in attempt prevented for employee {} on date {}", employee.getEmployeeCode(), businessDate);
            throw new DuplicateResourceException(
                    "Employee " + employee.getEmployeeCode() + " has already clocked in for date " + businessDate + "."
            );
        }
    }

    @Transactional
    public AttendanceResponse clockOut(UserPrincipal principal, ClockOutRequest request) {
        Employee employee = getEmployee(principal.getPublicId());

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new AccessDeniedException("Inactive employee account cannot perform attendance actions.");
        }

        AttendanceRecord openRecord = attendanceRepository.findByEmployeeIdAndStatus(employee.getId(), AttendanceStatus.OPEN)
                .orElseThrow(() -> new InvalidOperationException("No open attendance session found to clock out."));

        Instant clockOutTime = businessTimeService.getCurrentInstant();

        if (clockOutTime.isBefore(openRecord.getClockInTime())) {
            throw new InvalidOperationException("Clock-out time cannot be earlier than clock-in time.");
        }

        long minutes = Duration.between(openRecord.getClockInTime(), clockOutTime).toMinutes();
        openRecord.setClockOutTime(clockOutTime);
        openRecord.setDurationMinutes((int) minutes);
        openRecord.setStatus(AttendanceStatus.COMPLETED);

        if (request != null && request.notes() != null && !request.notes().trim().isEmpty()) {
            String existingNotes = openRecord.getNotes();
            openRecord.setNotes(existingNotes != null ? existingNotes + " | Out: " + request.notes() : request.notes());
        }

        AttendanceRecord updated = attendanceRepository.save(openRecord);
        log.info("Clock-out recorded for employee {}. Duration: {} minutes", employee.getEmployeeCode(), minutes);

        eventPublisher.publishEvent(new AuditEvent(
                employee.getPublicId(), employee.getEmail(), "CLOCK_OUT", "ATTENDANCE", updated.getPublicId().toString(), "Duration: " + minutes + "m"
        ));

        return AttendanceResponse.from(updated);
    }

    public PageResponse<AttendanceResponse> getMyAttendance(UserPrincipal principal,
                                                           LocalDate startDate,
                                                           LocalDate endDate,
                                                           Pageable pageable) {
        validateDateRange(startDate, endDate);
        Page<AttendanceRecord> page = attendanceRepository.findByEmployeeAndDateRange(
                principal.getInternalId(), startDate, endDate, pageable
        );
        return PageResponse.from(page.map(AttendanceResponse::from));
    }

    public AttendanceResponse getAttendanceById(UUID id, UserPrincipal principal) {
        AttendanceRecord record = attendanceRepository.findByPublicIdWithEmployee(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record with ID '" + id + "' was not found."));

        boolean isPrivileged = principal.getRole() == EmployeeRole.ADMIN || principal.getRole() == EmployeeRole.MANAGER;
        boolean isOwner = record.getEmployee().getPublicId().equals(principal.getPublicId());

        if (!isPrivileged && !isOwner) {
            throw new AccessDeniedException("You are not authorized to view this attendance record.");
        }

        return AttendanceResponse.from(record);
    }

    public PageResponse<AttendanceResponse> getAttendanceByEmployee(UUID employeePublicId,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate,
                                                                   Pageable pageable,
                                                                   UserPrincipal principal) {
        validateDateRange(startDate, endDate);
        boolean isPrivileged = principal.getRole() == EmployeeRole.ADMIN || principal.getRole() == EmployeeRole.MANAGER;
        boolean isOwner = employeePublicId.equals(principal.getPublicId());

        if (!isPrivileged && !isOwner) {
            throw new AccessDeniedException("You are not authorized to view another employee's attendance records.");
        }

        Employee employee = getEmployee(employeePublicId);
        Page<AttendanceRecord> page = attendanceRepository.findByEmployeeAndDateRange(
                employee.getId(), startDate, endDate, pageable
        );
        return PageResponse.from(page.map(AttendanceResponse::from));
    }

    public PageResponse<AttendanceResponse> listAttendance(LocalDate date,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          UUID employeePublicId,
                                                          AttendanceStatus status,
                                                          String search,
                                                          Pageable pageable) {
        validateDateRange(startDate, endDate);
        Page<AttendanceRecord> page = attendanceRepository.findAllWithFiltersAndSearch(
                employeePublicId, date, startDate, endDate, status,
                search == null || search.isBlank() ? null : search.trim(), pageable
        );
        return PageResponse.from(page.map(AttendanceResponse::from));
    }

    @Transactional
    public AttendanceResponse correctAttendance(UUID id, AttendanceCorrectionRequest request, UserPrincipal principal) {
        if (principal.getRole() != EmployeeRole.ADMIN && principal.getRole() != EmployeeRole.MANAGER) {
            throw new AccessDeniedException("Only managers and administrators can correct attendance.");
        }
        AttendanceRecord record = attendanceRepository.findByPublicIdWithEmployee(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record with ID '" + id + "' was not found."));
        Instant clockIn = request.clockInTime() != null ? request.clockInTime() : record.getClockInTime();
        Instant clockOut = request.clockOutTime();
        if (clockOut != null && clockOut.isBefore(clockIn)) {
            throw new InvalidOperationException("Clock-out time cannot be earlier than clock-in time.");
        }
        record.setClockInTime(clockIn);
        record.setClockOutTime(clockOut);
        record.setDurationMinutes(clockOut == null ? null : Math.toIntExact(Duration.between(clockIn, clockOut).toMinutes()));
        record.setStatus(clockOut == null ? AttendanceStatus.OPEN : AttendanceStatus.COMPLETED);
        record.setNotes((record.getNotes() == null ? "" : record.getNotes() + " | ")
                + "Correction: " + request.reason().trim());
        AttendanceRecord saved = attendanceRepository.save(record);
        eventPublisher.publishEvent(new AuditEvent(
                principal.getPublicId(), principal.getUsername(), "ATTENDANCE_CORRECTION",
                "ATTENDANCE", saved.getPublicId().toString(), request.reason().trim()
        ));
        return AttendanceResponse.from(saved);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidOperationException("startDate cannot be after endDate.");
        }
    }
}
