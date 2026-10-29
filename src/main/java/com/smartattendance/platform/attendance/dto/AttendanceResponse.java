package com.smartattendance.platform.attendance.dto;

import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        LocalDate attendanceDate,
        Instant clockInTime,
        Instant clockOutTime,
        Integer durationMinutes,
        AttendanceStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static AttendanceResponse from(AttendanceRecord record) {
        return new AttendanceResponse(
                record.getPublicId(),
                record.getEmployee().getPublicId(),
                record.getEmployee().getEmployeeCode(),
                record.getEmployee().getFullName(),
                record.getAttendanceDate(),
                record.getClockInTime(),
                record.getClockOutTime(),
                record.getDurationMinutes(),
                record.getStatus(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}

