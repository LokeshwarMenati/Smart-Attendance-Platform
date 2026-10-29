package com.smartattendance.platform.attendance;

import com.smartattendance.platform.attendance.dto.AttendanceResponse;
import com.smartattendance.platform.attendance.dto.ClockInRequest;
import com.smartattendance.platform.attendance.dto.ClockOutRequest;
import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.attendance.service.AttendanceService;
import com.smartattendance.platform.common.exception.DuplicateResourceException;
import com.smartattendance.platform.common.exception.InvalidOperationException;
import com.smartattendance.platform.common.util.BusinessTimeService;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.model.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BusinessTimeService businessTimeService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private AttendanceService attendanceService;

    private Employee activeEmployee;
    private UserPrincipal activePrincipal;
    private Instant fixedInstant;
    private LocalDate fixedDate;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(attendanceRepository, employeeRepository, businessTimeService, eventPublisher);

        UUID publicId = UUID.randomUUID();
        activeEmployee = new Employee(
                publicId, "EMP-500", "David", "Kim",
                "david@example.com", "hash", EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        );

        activePrincipal = new UserPrincipal(
                1L, publicId, "EMP-500", "david@example.com", "hash",
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE, Collections.emptyList()
        );

        fixedInstant = Instant.parse("2026-10-28T09:00:00Z");
        fixedDate = LocalDate.parse("2026-10-28");
    }

    @Test
    @DisplayName("Successfully clock in when no prior attendance exists for today")
    void clockInSuccess() {
        when(employeeRepository.findByPublicId(activePrincipal.getPublicId())).thenReturn(Optional.of(activeEmployee));
        when(businessTimeService.getCurrentInstant()).thenReturn(fixedInstant);
        when(businessTimeService.getBusinessDate(fixedInstant)).thenReturn(fixedDate);
        when(attendanceRepository.findByEmployeeAndAttendanceDate(activeEmployee, fixedDate)).thenReturn(Optional.empty());
        when(attendanceRepository.findByEmployeeIdAndStatus(activeEmployee.getId(), AttendanceStatus.OPEN)).thenReturn(Optional.empty());
        when(attendanceRepository.saveAndFlush(any(AttendanceRecord.class))).thenAnswer(i -> i.getArgument(0));

        AttendanceResponse response = attendanceService.clockIn(activePrincipal, new ClockInRequest("Morning shift"));

        assertThat(response).isNotNull();
        assertThat(response.attendanceDate()).isEqualTo(fixedDate);
        assertThat(response.clockInTime()).isEqualTo(fixedInstant);
        assertThat(response.status()).isEqualTo(AttendanceStatus.OPEN);
        assertThat(response.notes()).isEqualTo("Morning shift");
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when employee attempts duplicate clock-in on same date")
    void clockInDuplicateDate() {
        when(employeeRepository.findByPublicId(activePrincipal.getPublicId())).thenReturn(Optional.of(activeEmployee));
        when(businessTimeService.getCurrentInstant()).thenReturn(fixedInstant);
        when(businessTimeService.getBusinessDate(fixedInstant)).thenReturn(fixedDate);

        AttendanceRecord existingRecord = new AttendanceRecord(
                UUID.randomUUID(), activeEmployee, fixedDate, fixedInstant, "First shift"
        );
        when(attendanceRepository.findByEmployeeAndAttendanceDate(activeEmployee, fixedDate))
                .thenReturn(Optional.of(existingRecord));

        assertThatThrownBy(() -> attendanceService.clockIn(activePrincipal, new ClockInRequest("Second shift")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already clocked in for date");
    }

    @Test
    @DisplayName("Throw AccessDeniedException when inactive employee attempts to clock in")
    void clockInInactiveEmployee() {
        Employee inactiveEmployee = new Employee(
                activePrincipal.getPublicId(), "EMP-501", "Inactive", "User",
                "inactive@example.com", "hash", EmployeeRole.EMPLOYEE, EmployeeStatus.INACTIVE
        );
        when(employeeRepository.findByPublicId(activePrincipal.getPublicId())).thenReturn(Optional.of(inactiveEmployee));

        assertThatThrownBy(() -> attendanceService.clockIn(activePrincipal, new ClockInRequest("Clock in")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Inactive employee account");
    }

    @Test
    @DisplayName("Successfully clock out and calculate total working duration in minutes")
    void clockOutSuccess() {
        AttendanceRecord openRecord = new AttendanceRecord(
                UUID.randomUUID(), activeEmployee, fixedDate, fixedInstant, "Clocked in"
        );

        Instant clockOutInstant = fixedInstant.plus(8, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);

        when(employeeRepository.findByPublicId(activePrincipal.getPublicId())).thenReturn(Optional.of(activeEmployee));
        when(attendanceRepository.findByEmployeeIdAndStatus(activeEmployee.getId(), AttendanceStatus.OPEN))
                .thenReturn(Optional.of(openRecord));
        when(businessTimeService.getCurrentInstant()).thenReturn(clockOutInstant);
        when(attendanceRepository.save(any(AttendanceRecord.class))).thenAnswer(i -> i.getArgument(0));

        AttendanceResponse response = attendanceService.clockOut(activePrincipal, new ClockOutRequest("End of day"));

        assertThat(response.status()).isEqualTo(AttendanceStatus.COMPLETED);
        assertThat(response.clockOutTime()).isEqualTo(clockOutInstant);
        assertThat(response.durationMinutes()).isEqualTo(510); // 8 hours 30 minutes = 510 minutes
        verify(attendanceRepository).save(openRecord);
    }

    @Test
    @DisplayName("Throw InvalidOperationException when clocking out without an open session")
    void clockOutWithoutOpenSession() {
        when(employeeRepository.findByPublicId(activePrincipal.getPublicId())).thenReturn(Optional.of(activeEmployee));
        when(attendanceRepository.findByEmployeeIdAndStatus(activeEmployee.getId(), AttendanceStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockOut(activePrincipal, new ClockOutRequest("Out")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("No open attendance session found");
    }
}

