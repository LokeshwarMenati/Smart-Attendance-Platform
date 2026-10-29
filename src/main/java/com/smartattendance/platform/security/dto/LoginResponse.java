package com.smartattendance.platform.security.dto;

import com.smartattendance.platform.employee.dto.EmployeeResponse;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        EmployeeResponse employee
) {}

