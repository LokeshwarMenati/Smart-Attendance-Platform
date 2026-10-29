package com.smartattendance.platform.employee.dto;

import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateEmployeeRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotNull(message = "Role is required")
        EmployeeRole role,

        @NotNull(message = "Status is required")
        EmployeeStatus status
) {}

