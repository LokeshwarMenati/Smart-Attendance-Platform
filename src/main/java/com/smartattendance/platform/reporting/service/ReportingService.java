package com.smartattendance.platform.reporting.service;

import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.common.exception.InvalidOperationException;
import com.smartattendance.platform.common.util.BusinessTimeService;
import com.smartattendance.platform.common.util.CsvSanitizer;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.reporting.dto.AttendanceSummaryDto;
import com.smartattendance.platform.reporting.dto.DailyAttendanceReportDto;
import com.smartattendance.platform.reporting.dto.AttendanceAnalyticsDto;
import com.smartattendance.platform.security.model.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private static final int MAX_EXPORT_RECORDS = 10000;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final BusinessTimeService businessTimeService;
    private final java.time.LocalTime officeStartTime;
    private final int lateThresholdMinutes;

    public ReportingService(AttendanceRepository attendanceRepository,
                            EmployeeRepository employeeRepository,
                            BusinessTimeService businessTimeService,
                            @Value("${app.business.office-start-time:09:00}") java.time.LocalTime officeStartTime,
                            @Value("${app.business.late-threshold-minutes:15}") int lateThresholdMinutes) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.businessTimeService = businessTimeService;
        this.officeStartTime = officeStartTime;
        this.lateThresholdMinutes = lateThresholdMinutes;
    }

    public DailyAttendanceReportDto getDailyReport(LocalDate date, UserPrincipal principal) {
        if (principal.getRole() == EmployeeRole.EMPLOYEE) {
            throw new AccessDeniedException("Employees are not authorized to view enterprise daily reports.");
        }

        LocalDate targetDate = date != null ? date : businessTimeService.getCurrentBusinessDate();

        long totalPresent = attendanceRepository.countByAttendanceDate(targetDate);
        long completed = attendanceRepository.countByAttendanceDateAndStatus(targetDate, AttendanceStatus.COMPLETED);
        long open = attendanceRepository.countByAttendanceDateAndStatus(targetDate, AttendanceStatus.OPEN);
        long totalDuration = attendanceRepository.sumDurationMinutesByDate(targetDate);
        double avgDuration = attendanceRepository.avgDurationMinutesByDate(targetDate);

        return new DailyAttendanceReportDto(
                targetDate, totalPresent, completed, open, totalDuration, Math.round(avgDuration * 100.0) / 100.0
        );
    }

    public AttendanceSummaryDto getSummary(LocalDate startDate,
                                          LocalDate endDate,
                                          UUID employeePublicId,
                                          UserPrincipal principal) {
        validateDateRange(startDate, endDate);

        Long internalEmployeeId = resolveTargetEmployeeId(employeePublicId, principal);

        long total = attendanceRepository.countInRange(internalEmployeeId, startDate, endDate);
        long completed = attendanceRepository.countInRangeByStatus(internalEmployeeId, startDate, endDate, AttendanceStatus.COMPLETED);
        long open = attendanceRepository.countInRangeByStatus(internalEmployeeId, startDate, endDate, AttendanceStatus.OPEN);
        long totalDuration = attendanceRepository.sumDurationInRange(internalEmployeeId, startDate, endDate);
        double avgDuration = attendanceRepository.avgDurationInRange(internalEmployeeId, startDate, endDate);

        return new AttendanceSummaryDto(
                startDate, endDate, total, completed, open, totalDuration, Math.round(avgDuration * 100.0) / 100.0
        );
    }

    public AttendanceAnalyticsDto getAnalytics(UserPrincipal principal) {
        if (principal.getRole() == EmployeeRole.EMPLOYEE) {
            throw new AccessDeniedException("Employees are not authorized to view enterprise analytics.");
        }
        LocalDate today = businessTimeService.getCurrentBusinessDate();
        long totalEmployees = employeeRepository.countByStatus(com.smartattendance.platform.employee.entity.EmployeeStatus.ACTIVE);
        long present = attendanceRepository.countByAttendanceDate(today);
        long open = attendanceRepository.countByAttendanceDateAndStatus(today, AttendanceStatus.OPEN);
        Instant lateThreshold = today.atTime(officeStartTime.plusMinutes(lateThresholdMinutes))
                .atZone(businessTimeService.getBusinessZoneId()).toInstant();
        long late = attendanceRepository.countByAttendanceDateAndClockInTimeAfter(today, lateThreshold);
        LocalDate trendStart = today.minusDays(6);
        List<AttendanceAnalyticsDto.TrendPoint> trend = new java.util.ArrayList<>();
        for (LocalDate date = trendStart; !date.isAfter(today); date = date.plusDays(1)) {
            trend.add(new AttendanceAnalyticsDto.TrendPoint(date,
                    attendanceRepository.countByAttendanceDate(date),
                    attendanceRepository.countByAttendanceDateAndStatus(date, AttendanceStatus.COMPLETED),
                    attendanceRepository.countByAttendanceDateAndStatus(date, AttendanceStatus.OPEN),
                    attendanceRepository.sumDurationMinutesByDate(date)));
        }
        Map<String, Long> hours = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRepository.findCompletedBetween(today.minusDays(29), today)) {
            String key = record.getEmployee().getEmployeeCode() + "|" + record.getEmployee().getFullName();
            hours.merge(key, record.getDurationMinutes() == null ? 0L : record.getDurationMinutes().longValue(), Long::sum);
        }
        List<AttendanceAnalyticsDto.EmployeeHours> employeeHours = hours.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new AttendanceAnalyticsDto.EmployeeHours(parts[1], parts[0], entry.getValue());
                }).toList();
        double percentage = totalEmployees == 0 ? 0 : (present * 100.0) / totalEmployees;
        return new AttendanceAnalyticsDto(today, totalEmployees, present,
                Math.max(0, totalEmployees - present), late, open, Math.max(0, present - open),
                Math.round(percentage * 100.0) / 100.0, trend, employeeHours);
    }

    public void exportAttendanceCsv(LocalDate startDate,
                                   LocalDate endDate,
                                   UUID employeePublicId,
                                   UserPrincipal principal,
                                   Writer writer) throws IOException {
        validateDateRange(startDate, endDate);

        Long internalEmployeeId = resolveTargetEmployeeId(employeePublicId, principal);

        List<AttendanceRecord> records = attendanceRepository.findForExport(internalEmployeeId, startDate, endDate);
        if (records.size() > MAX_EXPORT_RECORDS) {
            throw new InvalidOperationException("Export result exceeds maximum allowed threshold of " + MAX_EXPORT_RECORDS + " records.");
        }

        // CSV Header
        writer.write("Record ID,Employee Code,Employee Name,Date,Clock-In Time (UTC),Clock-Out Time (UTC),Duration (Minutes),Status,Notes\n");

        for (AttendanceRecord r : records) {
            StringBuilder sb = new StringBuilder();
            sb.append(CsvSanitizer.escapeForCsv(r.getPublicId().toString())).append(",");
            sb.append(CsvSanitizer.escapeForCsv(r.getEmployee().getEmployeeCode())).append(",");
            sb.append(CsvSanitizer.escapeForCsv(r.getEmployee().getFullName())).append(",");
            sb.append(CsvSanitizer.escapeForCsv(r.getAttendanceDate().toString())).append(",");
            sb.append(CsvSanitizer.escapeForCsv(r.getClockInTime().toString())).append(",");
            sb.append(CsvSanitizer.escapeForCsv(r.getClockOutTime() != null ? r.getClockOutTime().toString() : "")).append(",");
            sb.append(r.getDurationMinutes() != null ? r.getDurationMinutes() : "").append(",");
            sb.append(CsvSanitizer.escapeForCsv(r.getStatus().name())).append(",");
            sb.append(CsvSanitizer.escapeForCsv(r.getNotes() != null ? r.getNotes() : ""));
            sb.append("\n");
            writer.write(sb.toString());
        }

        writer.flush();
    }

    private Long resolveTargetEmployeeId(UUID employeePublicId, UserPrincipal principal) {
        boolean isPrivileged = principal.getRole() == EmployeeRole.ADMIN || principal.getRole() == EmployeeRole.MANAGER;

        if (employeePublicId != null) {
            if (!isPrivileged && !employeePublicId.equals(principal.getPublicId())) {
                throw new AccessDeniedException("Employees are only permitted to view their own attendance reports.");
            }
            Employee emp = employeeRepository.findByPublicId(employeePublicId)
                    .orElseThrow(() -> new InvalidOperationException("Employee with ID '" + employeePublicId + "' was not found."));
            return emp.getId();
        }

        if (!isPrivileged) {
            return principal.getInternalId();
        }

        return null; // All employees for managers/admins
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidOperationException("startDate cannot be after endDate.");
        }
    }
}
