package com.smartattendance.platform.attendance.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record AttendanceCorrectionRequest(
        Instant clockInTime,
        Instant clockOutTime,
        @NotBlank(message = "A correction reason is required")
        String reason
) {}
