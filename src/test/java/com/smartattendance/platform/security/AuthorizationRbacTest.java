package com.smartattendance.platform.security;

import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.jwt.JwtProperties;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private com.smartattendance.platform.attendance.repository.AttendanceRepository attendanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Employee employee1;
    private Employee employee2;
    private Employee admin;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();

        employee1 = employeeRepository.save(new Employee(
                UUID.randomUUID(), "EMP-001", "User", "One",
                "user1@example.com", passwordEncoder.encode("Secret123!"),
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        ));

        employee2 = employeeRepository.save(new Employee(
                UUID.randomUUID(), "EMP-002", "User", "Two",
                "user2@example.com", passwordEncoder.encode("Secret123!"),
                EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        ));

        admin = employeeRepository.save(new Employee(
                UUID.randomUUID(), "ADM-001", "Admin", "User",
                "admin@example.com", passwordEncoder.encode("Secret123!"),
                EmployeeRole.ADMIN, EmployeeStatus.ACTIVE
        ));
    }

    @Test
    @DisplayName("Employee cannot access another employee's profile by changing UUID in URL (403 Forbidden)")
    void employeeCannotAccessOtherEmployee() throws Exception {
        String tokenEmployee1 = jwtTokenProvider.generateToken(
                employee1.getPublicId(),
                employee1.getEmail(),
                employee1.getRole().name(),
                employee1.getEmployeeCode()
        );

        // Employee 1 attempts to access Employee 2's profile
        mockMvc.perform(get("/api/v1/employees/{id}", employee2.getPublicId())
                        .header("Authorization", "Bearer " + tokenEmployee1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Access Forbidden"));
    }

    @Test
    @DisplayName("Employee can access their own profile by UUID")
    void employeeCanAccessOwnProfile() throws Exception {
        String tokenEmployee1 = jwtTokenProvider.generateToken(
                employee1.getPublicId(),
                employee1.getEmail(),
                employee1.getRole().name(),
                employee1.getEmployeeCode()
        );

        mockMvc.perform(get("/api/v1/employees/{id}", employee1.getPublicId())
                        .header("Authorization", "Bearer " + tokenEmployee1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user1@example.com"));
    }

    @Test
    @DisplayName("Admin can access any employee's profile and perform administrative actions")
    void adminCanAccessAnyProfile() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(
                admin.getPublicId(),
                admin.getEmail(),
                admin.getRole().name(),
                admin.getEmployeeCode()
        );

        mockMvc.perform(get("/api/v1/employees/{id}", employee2.getPublicId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user2@example.com"));

        mockMvc.perform(patch("/api/v1/employees/{id}/deactivate", employee2.getPublicId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("Expired JWT token is rejected with 401 Unauthorized")
    void expiredTokenRejected() throws Exception {
        // Create a provider with 0ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                new JwtProperties("4c9e89d6b2a14e9f848a3182d3856bcf2911b617d5a570087799b82873130c24", -1000)
        );

        String expiredToken = shortLivedProvider.generateToken(
                employee1.getPublicId(), employee1.getEmail(), employee1.getRole().name(), employee1.getEmployeeCode()
        );

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }
}

