package com.smartattendance.platform.audit;

import com.smartattendance.platform.attendance.dto.ClockInRequest;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.audit.entity.AuditLog;
import com.smartattendance.platform.audit.repository.AuditLogRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Employee admin;
    private Employee employee;
    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();

        admin = employeeRepository.save(new Employee(
                UUID.randomUUID(), "ADM-999", "Audit", "Admin",
                "admin.audit@example.com", passwordEncoder.encode("Pass123!"),
                EmployeeRole.ADMIN, EmployeeStatus.ACTIVE
        ));

        employee = employeeRepository.save(new Employee(
                UUID.randomUUID(), "EMP-999", "Audit", "User",
                "user.audit@example.com", passwordEncoder.encode("Pass123!"),
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        ));

        adminToken = jwtTokenProvider.generateToken(
                admin.getPublicId(), admin.getEmail(), admin.getRole().name(), admin.getEmployeeCode()
        );
        employeeToken = jwtTokenProvider.generateToken(
                employee.getPublicId(), employee.getEmail(), employee.getRole().name(), employee.getEmployeeCode()
        );
    }

    @Test
    @DisplayName("Clock-in triggers audit event and stores audit log entry")
    void clockInGeneratesAuditLog() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\": \"Audit test clock-in\"}"))
                .andExpect(status().isCreated());

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();
        assertThat(logs).anyMatch(log -> "CLOCK_IN".equals(log.getAction()) &&
                employee.getEmail().equals(log.getActorEmail()));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs - Admin can query audit records")
    void adminCanQueryAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs - Employee is forbidden (403)")
    void employeeCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }
}

