package com.smartattendance.platform.reporting;

import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Employee employee;
    private Employee manager;
    private String employeeToken;
    private String managerToken;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();

        employee = employeeRepository.save(new Employee(
                UUID.randomUUID(), "EMP-801", "Bruce", "Wayne",
                "bruce@wayne.com", passwordEncoder.encode("Secret123!"),
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        ));

        manager = employeeRepository.save(new Employee(
                UUID.randomUUID(), "MGR-801", "Alfred", "Pennyworth",
                "alfred@wayne.com", passwordEncoder.encode("Secret123!"),
                EmployeeRole.MANAGER, EmployeeStatus.ACTIVE
        ));

        employeeToken = jwtTokenProvider.generateToken(
                employee.getPublicId(), employee.getEmail(), employee.getRole().name(), employee.getEmployeeCode()
        );
        managerToken = jwtTokenProvider.generateToken(
                manager.getPublicId(), manager.getEmail(), manager.getRole().name(), manager.getEmployeeCode()
        );

        // Seed 1 completed session and 1 open session
        AttendanceRecord completedRecord = new AttendanceRecord(
                UUID.randomUUID(), employee, LocalDate.of(2026, 10, 27),
                Instant.parse("2026-10-27T09:00:00Z"), "=SUM(A1:A10), Special Notes \"with quotes\""
        );
        completedRecord.setClockOutTime(Instant.parse("2026-10-27T17:00:00Z"));
        completedRecord.setDurationMinutes(480);
        completedRecord.setStatus(AttendanceStatus.COMPLETED);
        attendanceRepository.save(completedRecord);

        AttendanceRecord openRecord = new AttendanceRecord(
                UUID.randomUUID(), employee, LocalDate.of(2026, 10, 28),
                Instant.parse("2026-10-28T09:00:00Z"), "Today open shift"
        );
        attendanceRepository.save(openRecord);
    }

    @Test
    @DisplayName("GET /api/v1/reports/attendance/daily - Manager retrieves aggregated metrics for specific date")
    void getDailyReportAsManager() throws Exception {
        mockMvc.perform(get("/api/v1/reports/attendance/daily")
                        .param("date", "2026-10-27")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-27"))
                .andExpect(jsonPath("$.totalPresent").value(1))
                .andExpect(jsonPath("$.completedSessions").value(1))
                .andExpect(jsonPath("$.openSessions").value(0))
                .andExpect(jsonPath("$.totalDurationMinutes").value(480));
    }

    @Test
    @DisplayName("GET /api/v1/reports/attendance/daily - Forbidden 403 for employee")
    void getDailyReportForbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/reports/attendance/daily")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/reports/attendance/summary - Computes summary stats correctly")
    void getSummaryStats() throws Exception {
        mockMvc.perform(get("/api/v1/reports/attendance/summary")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(2))
                .andExpect(jsonPath("$.completedSessions").value(1))
                .andExpect(jsonPath("$.openSessions").value(1))
                .andExpect(jsonPath("$.totalDurationMinutes").value(480))
                .andExpect(jsonPath("$.averageDurationMinutes").value(480.0));
    }

    @Test
    @DisplayName("GET /api/v1/reports/attendance/export - Exports CSV and sanitizes formula injection")
    void exportAttendanceCsvWithSanitization() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/reports/attendance/export")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=")))
                .andReturn();

        String csvContent = result.getResponse().getContentAsString();

        // 1. Check header
        assertThat(csvContent).contains("Record ID,Employee Code,Employee Name,Date,Clock-In Time (UTC),Clock-Out Time (UTC),Duration (Minutes),Status,Notes");

        // 2. Check formula injection protection: '=SUM' must be escaped to "'=SUM"
        assertThat(csvContent).contains("'=SUM(A1:A10)");

        // 3. Check quotes escaping
        assertThat(csvContent).contains("\"\"with quotes\"\"");
    }
}

