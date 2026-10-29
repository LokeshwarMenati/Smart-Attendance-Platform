package com.smartattendance.platform.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartattendance.platform.attendance.dto.ClockInRequest;
import com.smartattendance.platform.attendance.dto.ClockOutRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Employee employee1;
    private Employee employee2;
    private Employee manager;
    private String tokenEmployee1;
    private String tokenEmployee2;
    private String tokenManager;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();

        employee1 = employeeRepository.save(new Employee(
                UUID.randomUUID(), "EMP-601", "Emma", "Stone",
                "emma@example.com", passwordEncoder.encode("Pass123!"),
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        ));

        employee2 = employeeRepository.save(new Employee(
                UUID.randomUUID(), "EMP-602", "Ryan", "Gosling",
                "ryan@example.com", passwordEncoder.encode("Pass123!"),
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        ));

        manager = employeeRepository.save(new Employee(
                UUID.randomUUID(), "MGR-601", "Meryl", "Streep",
                "manager@example.com", passwordEncoder.encode("Pass123!"),
                EmployeeRole.MANAGER, EmployeeStatus.ACTIVE
        ));

        tokenEmployee1 = jwtTokenProvider.generateToken(
                employee1.getPublicId(), employee1.getEmail(), employee1.getRole().name(), employee1.getEmployeeCode()
        );
        tokenEmployee2 = jwtTokenProvider.generateToken(
                employee2.getPublicId(), employee2.getEmail(), employee2.getRole().name(), employee2.getEmployeeCode()
        );
        tokenManager = jwtTokenProvider.generateToken(
                manager.getPublicId(), manager.getEmail(), manager.getRole().name(), manager.getEmployeeCode()
        );
    }

    @Test
    @DisplayName("Complete Flow: Clock-In followed by Clock-Out succeeds")
    void clockInAndClockOutFlow() throws Exception {
        // 1. Clock In
        ClockInRequest inRequest = new ClockInRequest("Starting morning shift");
        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .header("Authorization", "Bearer " + tokenEmployee1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.employeeCode").value("EMP-601"))
                .andExpect(jsonPath("$.clockInTime").isNotEmpty());

        // 2. Clock Out
        ClockOutRequest outRequest = new ClockOutRequest("Ending work day");
        mockMvc.perform(post("/api/v1/attendance/clock-out")
                        .header("Authorization", "Bearer " + tokenEmployee1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.clockOutTime").isNotEmpty())
                .andExpect(jsonPath("$.durationMinutes", greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Clock-in rejected with 409 Conflict if attempted twice on same date")
    void duplicateClockInRejected() throws Exception {
        ClockInRequest inRequest = new ClockInRequest("First clock-in");

        // First clock-in succeeds
        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .header("Authorization", "Bearer " + tokenEmployee1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inRequest)))
                .andExpect(status().isCreated());

        // Second clock-in fails with 409 Conflict
        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .header("Authorization", "Bearer " + tokenEmployee1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    @DisplayName("Clock-out rejected with 400 Bad Request when no open session exists")
    void clockOutWithoutClockInFails() throws Exception {
        ClockOutRequest outRequest = new ClockOutRequest("Trying to clock out");

        mockMvc.perform(post("/api/v1/attendance/clock-out")
                        .header("Authorization", "Bearer " + tokenEmployee1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Business Operation"));
    }

    @Test
    @DisplayName("Repeated clock-out rejected when session is already completed")
    void repeatedClockOutFails() throws Exception {
        // Clock in
        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .header("Authorization", "Bearer " + tokenEmployee1))
                .andExpect(status().isCreated());

        // First clock out
        mockMvc.perform(post("/api/v1/attendance/clock-out")
                        .header("Authorization", "Bearer " + tokenEmployee1))
                .andExpect(status().isOk());

        // Second clock out fails
        mockMvc.perform(post("/api/v1/attendance/clock-out")
                        .header("Authorization", "Bearer " + tokenEmployee1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No open attendance session found to clock out."));
    }

    @Test
    @DisplayName("Employee cannot view another employee's attendance history (403 Forbidden)")
    void crossEmployeeAttendanceForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/employee/{employeeId}", employee2.getPublicId())
                        .header("Authorization", "Bearer " + tokenEmployee1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager can view any employee's attendance history")
    void managerCanViewEmployeeAttendance() throws Exception {
        // Clock in for Employee 2
        mockMvc.perform(post("/api/v1/attendance/clock-in")
                        .header("Authorization", "Bearer " + tokenEmployee2))
                .andExpect(status().isCreated());

        // Manager queries Employee 2's attendance
        mockMvc.perform(get("/api/v1/attendance/employee/{employeeId}", employee2.getPublicId())
                        .header("Authorization", "Bearer " + tokenManager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-602"));
    }
}

