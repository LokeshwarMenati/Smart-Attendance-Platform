package com.smartattendance.platform.employee.dto;

import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.entity.EmployeeShift;

import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String employeeCode,
        String firstName,
        String lastName,
        String fullName,
        String email,
        EmployeeRole role,
        EmployeeStatus status,
        EmployeeShift shift,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getPublicId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getStatus(),
                employee.getShift(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
