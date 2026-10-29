package com.smartattendance.platform.reporting.dto;

import java.time.LocalDate;

public record DailyAttendanceReportDto(
        LocalDate date,
        long totalPresent,
        long completedSessions,
        long openSessions,
        long totalDurationMinutes,
        double averageDurationMinutes
) {}

