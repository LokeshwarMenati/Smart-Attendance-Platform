package com.smartattendance.platform.attendance;

import com.smartattendance.platform.attendance.dto.CorrectionRequestCreate;
import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.repository.AttendanceCorrectionRequestRepository;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.attendance.service.AttendanceCorrectionService;
import com.smartattendance.platform.common.exception.InvalidOperationException;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.model.UserPrincipal;
import com.smartattendance.platform.notification.service.NotificationService;
import com.smartattendance.platform.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceCorrectionServiceTest {
    @Mock AttendanceCorrectionRequestRepository requestRepository;
    @Mock AttendanceRepository attendanceRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock NotificationRepository notificationRepository;

    private AttendanceCorrectionService service;
    private Employee employee;
    private UserPrincipal principal;
    private AttendanceRecord record;

    @BeforeEach
    void setUp() {
        service = new AttendanceCorrectionService(requestRepository, attendanceRepository, employeeRepository, eventPublisher,
                new NotificationService(notificationRepository, employeeRepository));
        UUID employeeId = UUID.randomUUID();
        employee = new Employee(employeeId, "EMP-901", "Test", "Employee", "test@example.com",
                "hash", EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE);
        principal = new UserPrincipal(1L, employeeId, "EMP-901", employee.getEmail(), "hash",
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE, Collections.emptyList());
        record = new AttendanceRecord(UUID.randomUUID(), employee, LocalDate.now(),
                Instant.parse("2026-10-29T09:00:00Z"), null);
    }

    @Test
    void rejectsInvalidProposedTimeRange() {
        when(attendanceRepository.findByPublicIdWithEmployee(record.getPublicId())).thenReturn(Optional.of(record));
        CorrectionRequestCreate request = new CorrectionRequestCreate(
                Instant.parse("2026-10-29T17:00:00Z"),
                Instant.parse("2026-10-29T09:00:00Z"), "Correction needed");

        assertThatThrownBy(() -> service.submit(record.getPublicId(), request, principal))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void submitsOwnCorrectionRequestWithMandatoryReason() {
        when(attendanceRepository.findByPublicIdWithEmployee(record.getPublicId())).thenReturn(Optional.of(record));
        when(requestRepository.existsByAttendanceIdAndStatus(any(), any())).thenReturn(false);
        when(employeeRepository.findByPublicId(principal.getPublicId())).thenReturn(Optional.of(employee));
        when(employeeRepository.findByRole(any())).thenReturn(Collections.emptyList());
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.submit(record.getPublicId(), new CorrectionRequestCreate(
                record.getClockInTime(), Instant.parse("2026-10-29T17:00:00Z"), "Missed clock-out"), principal);
    }
}
