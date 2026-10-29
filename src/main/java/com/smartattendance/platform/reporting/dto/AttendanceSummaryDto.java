package com.smartattendance.platform.reporting.dto;

import java.time.LocalDate;

public record AttendanceSummaryDto(
        LocalDate startDate,
        LocalDate endDate,
        long totalSessions,
        long completedSessions,
        long openSessions,
        long totalDurationMinutes,
        double averageDurationMinutes
) {}

