package com.smartattendance.platform.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CorrectionRequestCreate(
        @NotNull Instant proposedClockIn,
        Instant proposedClockOut,
        @NotBlank(message = "A correction reason is required") String reason
) {}
