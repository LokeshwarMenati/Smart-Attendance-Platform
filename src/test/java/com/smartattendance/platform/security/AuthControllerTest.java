package com.smartattendance.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.dto.LoginRequest;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private com.smartattendance.platform.attendance.repository.AttendanceRepository attendanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Employee activeEmployee;
    private Employee inactiveEmployee;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();

        activeEmployee = new Employee(
                UUID.randomUUID(),
                "EMP-701",
                "Alice",
                "Wonder",
                "alice.active@example.com",
                passwordEncoder.encode("Password123!"),
                EmployeeRole.EMPLOYEE,
                EmployeeStatus.ACTIVE
        );
        activeEmployee = employeeRepository.save(activeEmployee);

        inactiveEmployee = new Employee(
                UUID.randomUUID(),
                "EMP-702",
                "Bob",
                "Builder",
                "bob.inactive@example.com",
                passwordEncoder.encode("Password123!"),
                EmployeeRole.EMPLOYEE,
                EmployeeStatus.INACTIVE
        );
        inactiveEmployee = employeeRepository.save(inactiveEmployee);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Returns valid JWT token on correct credentials")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("alice.active@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.employee.email").value("alice.active@example.com"))
                .andExpect(jsonPath("$.employee.employeeCode").value("EMP-701"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Returns 401 Unauthorized on invalid password")
    void loginInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("alice.active@example.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Returns 401 Unauthorized for deactivated account")
    void loginInactiveAccount() throws Exception {
        LoginRequest request = new LoginRequest("bob.inactive@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Account Deactivated"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Successfully retrieves profile using Bearer token")
    void getCurrentUserWithValidToken() throws Exception {
        String token = jwtTokenProvider.generateToken(
                activeEmployee.getPublicId(),
                activeEmployee.getEmail(),
                activeEmployee.getRole().name(),
                activeEmployee.getEmployeeCode()
        );

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice.active@example.com"))
                .andExpect(jsonPath("$.employeeCode").value("EMP-701"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Fails with 401 when token is invalid or malformed")
    void getCurrentUserWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Fails with 401 when Authorization header is missing")
    void getCurrentUserMissingAuth() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}

