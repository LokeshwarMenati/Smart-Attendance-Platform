package com.smartattendance.platform.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public record CorrectionDecisionRequest(@NotBlank(message = "A decision reason is required") String reason) {}
