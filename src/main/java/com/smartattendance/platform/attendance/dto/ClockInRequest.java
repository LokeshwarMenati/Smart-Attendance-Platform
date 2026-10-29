package com.smartattendance.platform.attendance.dto;

import jakarta.validation.constraints.Size;

public record ClockInRequest(
        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
) {}

