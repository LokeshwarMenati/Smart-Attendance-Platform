package com.smartattendance.platform.reporting.controller;

import com.smartattendance.platform.reporting.dto.AttendanceSummaryDto;
import com.smartattendance.platform.reporting.dto.DailyAttendanceReportDto;
import com.smartattendance.platform.reporting.dto.AttendanceAnalyticsDto;
import com.smartattendance.platform.reporting.service.ReportingService;
import com.smartattendance.platform.security.model.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/attendance")
@Tag(name = "Reports & Analytics", description = "Endpoints for attendance aggregations and secure CSV exports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get daily attendance status report", description = "Provides daily attendance overview including open vs completed sessions.")
    public ResponseEntity<DailyAttendanceReportDto> getDailyReport(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reportingService.getDailyReport(date, principal));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get attendance analytics dashboard data")
    public ResponseEntity<AttendanceAnalyticsDto> getAnalytics(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reportingService.getAnalytics(principal));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get attendance duration and session summary")
    public ResponseEntity<AttendanceSummaryDto> getSummary(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "employeeId", required = false) UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(reportingService.getSummary(startDate, endDate, employeeId, principal));
    }

    @GetMapping("/export")
    @Operation(summary = "Export attendance records to CSV", description = "Generates and streams a CSV report sanitized against formula injection.")
    @ApiResponse(responseCode = "200", description = "CSV export stream")
    public void exportAttendanceCsv(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "employeeId", required = false) UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response
    ) throws IOException {
        String timestamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String filename = "attendance_report_" + timestamp + ".csv";

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        reportingService.exportAttendanceCsv(startDate, endDate, employeeId, principal, response.getWriter());
    }
}
