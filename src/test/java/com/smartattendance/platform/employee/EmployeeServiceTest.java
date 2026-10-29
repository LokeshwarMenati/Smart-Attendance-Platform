package com.smartattendance.platform.employee;

import com.smartattendance.platform.common.exception.DuplicateResourceException;
import com.smartattendance.platform.common.exception.ResourceNotFoundException;
import com.smartattendance.platform.employee.dto.CreateEmployeeRequest;
import com.smartattendance.platform.employee.dto.EmployeeResponse;
import com.smartattendance.platform.employee.dto.UpdateEmployeeRequest;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.employee.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, passwordEncoder, eventPublisher);
    }

    @Test
    @DisplayName("Successfully create employee with hashed password and normalized email")
    void createEmployeeSuccess() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-001",
                "Alex",
                "Rivera",
                " ALEX.Rivera@Example.Com ",
                "PlainSecret123!",
                EmployeeRole.EMPLOYEE
        );

        when(employeeRepository.existsByEmailIgnoreCase("alex.rivera@example.com")).thenReturn(false);
        when(employeeRepository.existsByEmployeeCodeIgnoreCase("EMP-001")).thenReturn(false);
        when(passwordEncoder.encode("PlainSecret123!")).thenReturn("$2a$12$hashedPasswordValue");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.createEmployee(request);

        assertThat(response).isNotNull();
        assertThat(response.employeeCode()).isEqualTo("EMP-001");
        assertThat(response.email()).isEqualTo("alex.rivera@example.com");
        assertThat(response.role()).isEqualTo(EmployeeRole.EMPLOYEE);
        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$12$hashedPasswordValue");
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when email already exists")
    void createEmployeeDuplicateEmail() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-002",
                "Jane",
                "Doe",
                "existing@example.com",
                "Secret123!",
                EmployeeRole.EMPLOYEE
        );

        when(employeeRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when employee code already exists")
    void createEmployeeDuplicateCode() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "EMP-DUPLICATE",
                "Jane",
                "Doe",
                "unique@example.com",
                "Secret123!",
                EmployeeRole.EMPLOYEE
        );

        when(employeeRepository.existsByEmailIgnoreCase("unique@example.com")).thenReturn(false);
        when(employeeRepository.existsByEmployeeCodeIgnoreCase("EMP-DUPLICATE")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when querying non-existent public ID")
    void getEmployeeNotFound() {
        UUID randomId = UUID.randomUUID();
        when(employeeRepository.findByPublicId(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeByPublicId(randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    @DisplayName("Successfully update employee details")
    void updateEmployeeSuccess() {
        UUID publicId = UUID.randomUUID();
        Employee employee = new Employee(
                publicId, "EMP-010", "OldName", "OldLast",
                "user@example.com", "hash", EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        );

        when(employeeRepository.findByPublicId(publicId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEmployeeRequest updateRequest = new UpdateEmployeeRequest(
                "NewName", "NewLast", EmployeeRole.MANAGER, EmployeeStatus.ACTIVE
        );

        EmployeeResponse response = employeeService.updateEmployee(publicId, updateRequest);

        assertThat(response.firstName()).isEqualTo("NewName");
        assertThat(response.lastName()).isEqualTo("NewLast");
        assertThat(response.role()).isEqualTo(EmployeeRole.MANAGER);
    }

    @Test
    @DisplayName("Successfully deactivate active employee")
    void deactivateEmployeeSuccess() {
        UUID publicId = UUID.randomUUID();
        Employee employee = new Employee(
                publicId, "EMP-011", "Active", "User",
                "active@example.com", "hash", EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE
        );

        when(employeeRepository.findByPublicId(publicId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.deactivateEmployee(publicId);

        assertThat(response.status()).isEqualTo(EmployeeStatus.INACTIVE);
    }
}

