package com.smartattendance.platform.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartattendance.platform.employee.dto.CreateEmployeeRequest;
import com.smartattendance.platform.employee.dto.UpdateEmployeeRequest;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private com.smartattendance.platform.attendance.repository.AttendanceRepository attendanceRepository;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        employeeRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/employees - Admin successfully creates employee")
    void createEmployeeAsAdmin() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-100",
                "Morgan",
                "Stanley",
                "morgan@example.com",
                "SecurePass123!",
                EmployeeRole.EMPLOYEE
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.employeeCode").value("EMP-100"))
                .andExpect(jsonPath("$.email").value("morgan@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/employees - Fails with 400 when validation rules violated")
    void createEmployeeValidationFailure() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "", // Blank code
                "", // Blank name
                "Last",
                "not-an-email", // Invalid email
                "short", // Password < 8 chars
                null // Null role
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failure"))
                .andExpect(jsonPath("$.validationErrors.employeeCode").exists())
                .andExpect(jsonPath("$.validationErrors.firstName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("POST /api/v1/employees - Forbidden 403 for non-admin user")
    void createEmployeeForbiddenForNonAdmin() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-101",
                "Morgan",
                "Stanley",
                "morgan2@example.com",
                "SecurePass123!",
                EmployeeRole.EMPLOYEE
        );

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GET /api/v1/employees - Returns paginated list of employees for manager")
    void listEmployeesAsManager() throws Exception {
        Employee emp = new Employee(
                UUID.randomUUID(),
                "EMP-200",
                "Alice",
                "Walker",
                "alice@example.com",
                "hash",
                EmployeeRole.EMPLOYEE,
                EmployeeStatus.ACTIVE
        );
        employeeRepository.save(emp);

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-200"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/v1/employees/{id}/deactivate - Successfully deactivates employee")
    void deactivateEmployeeAsAdmin() throws Exception {
        Employee emp = new Employee(
                UUID.randomUUID(),
                "EMP-300",
                "Bob",
                "Ross",
                "bob@example.com",
                "hash",
                EmployeeRole.EMPLOYEE,
                EmployeeStatus.ACTIVE
        );
        Employee saved = employeeRepository.save(emp);

        mockMvc.perform(patch("/api/v1/employees/{id}/deactivate", saved.getPublicId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}

