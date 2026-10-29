package com.smartattendance.platform.reporting.dto;

import java.time.LocalDate;
import java.util.List;

public record AttendanceAnalyticsDto(
        LocalDate date,
        long totalEmployees,
        long employeesPresent,
        long employeesAbsent,
        long lateArrivals,
        long currentlyClockedIn,
        long currentlyClockedOut,
        double attendancePercentage,
        List<TrendPoint> dailyTrend,
        List<EmployeeHours> employeeHours
) {
    public record TrendPoint(LocalDate date, long present, long completed, long open, long totalMinutes) {}

    public record EmployeeHours(String employeeName, String employeeCode, long totalMinutes) {}
}
