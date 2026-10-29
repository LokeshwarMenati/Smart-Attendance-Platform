package com.smartattendance.platform.employee.service;

import com.smartattendance.platform.audit.event.AuditEvent;
import com.smartattendance.platform.common.exception.DuplicateResourceException;
import com.smartattendance.platform.common.exception.ResourceNotFoundException;
import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.employee.dto.CreateEmployeeRequest;
import com.smartattendance.platform.employee.dto.EmployeeResponse;
import com.smartattendance.platform.employee.dto.UpdateEmployeeRequest;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeService(EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder,
                           ApplicationEventPublisher eventPublisher) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String trimmedCode = request.employeeCode().trim();

        if (employeeRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("An employee with email '" + normalizedEmail + "' already exists.");
        }

        if (employeeRepository.existsByEmployeeCodeIgnoreCase(trimmedCode)) {
            throw new DuplicateResourceException("An employee with code '" + trimmedCode + "' already exists.");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        Employee employee = new Employee(
                UUID.randomUUID(),
                trimmedCode,
                request.firstName().trim(),
                request.lastName().trim(),
                normalizedEmail,
                hashedPassword,
                request.role(),
                EmployeeStatus.ACTIVE
        );

        Employee saved = employeeRepository.save(employee);
        log.info("Created new employee with publicId: {} and code: {}", saved.getPublicId(), saved.getEmployeeCode());

        eventPublisher.publishEvent(new AuditEvent(
                null, null, "EMPLOYEE_CREATED", "EMPLOYEE", saved.getPublicId().toString(), "Code: " + saved.getEmployeeCode()
        ));

        return EmployeeResponse.from(saved);
    }

    public EmployeeResponse getEmployeeByPublicId(UUID publicId) {
        return EmployeeResponse.from(getEmployeeEntity(publicId));
    }

    public Employee getEmployeeEntity(UUID publicId) {
        return employeeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID '" + publicId + "' was not found."));
    }

    public PageResponse<EmployeeResponse> listEmployees(EmployeeStatus status, String search, Pageable pageable) {
        String cleanedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Employee> page = employeeRepository.findAllWithFilters(status, cleanedSearch, pageable);
        return PageResponse.from(page.map(EmployeeResponse::from));
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID publicId, UpdateEmployeeRequest request) {
        Employee employee = getEmployeeEntity(publicId);

        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setRole(request.role());
        employee.setStatus(request.status());

        Employee updated = employeeRepository.save(employee);
        log.info("Updated employee details for publicId: {}", publicId);

        eventPublisher.publishEvent(new AuditEvent(
                null, null, "EMPLOYEE_UPDATED", "EMPLOYEE", publicId.toString(), "Updated details"
        ));
        return EmployeeResponse.from(updated);
    }

    @Transactional
    public EmployeeResponse deactivateEmployee(UUID publicId) {
        Employee employee = getEmployeeEntity(publicId);
        employee.setStatus(EmployeeStatus.INACTIVE);

        Employee updated = employeeRepository.save(employee);
        log.info("Deactivated employee with publicId: {}", publicId);

        eventPublisher.publishEvent(new AuditEvent(
                null, null, "EMPLOYEE_DEACTIVATED", "EMPLOYEE", publicId.toString(), "Status set to INACTIVE"
        ));
        return EmployeeResponse.from(updated);
    }
}

