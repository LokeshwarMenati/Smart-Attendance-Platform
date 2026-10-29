package com.smartattendance.platform.attendance.controller;

import com.smartattendance.platform.attendance.dto.AttendanceResponse;
import com.smartattendance.platform.attendance.dto.ClockInRequest;
import com.smartattendance.platform.attendance.dto.ClockOutRequest;
import com.smartattendance.platform.attendance.dto.AttendanceCorrectionRequest;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.service.AttendanceService;
import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.security.model.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance Engine", description = "Core endpoints for clocking in/out, session records, and history")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in")
    @Operation(summary = "Clock in for the day", description = "Records clock-in for the authenticated user for the current business date.")
    @ApiResponse(responseCode = "201", description = "Clock-in recorded successfully")
    @ApiResponse(responseCode = "409", description = "Employee has already clocked in for this date")
    public ResponseEntity<AttendanceResponse> clockIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) ClockInRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.clockIn(principal, request));
    }

    @PostMapping("/clock-out")
    @Operation(summary = "Clock out of active session", description = "Clocks out of the open attendance session and calculates total duration.")
    @ApiResponse(responseCode = "200", description = "Clock-out recorded successfully")
    @ApiResponse(responseCode = "400", description = "No open session found or invalid clock-out state")
    public ResponseEntity<AttendanceResponse> clockOut(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) ClockOutRequest request
    ) {
        return ResponseEntity.ok(attendanceService.clockOut(principal, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get personal attendance history", description = "Returns the authenticated employee's attendance records with optional date filtering.")
    public ResponseEntity<PageResponse<AttendanceResponse>> getMyAttendance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(attendanceService.getMyAttendance(principal, startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attendance record by ID", description = "Retrieves attendance record by public UUID.")
    public ResponseEntity<AttendanceResponse> getAttendanceById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(attendanceService.getAttendanceById(id, principal));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or authentication.principal.publicId == #employeeId")
    @Operation(summary = "Get attendance history for specific employee")
    public ResponseEntity<PageResponse<AttendanceResponse>> getAttendanceByEmployee(
            @PathVariable("employeeId") UUID employeeId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEmployee(employeeId, startDate, endDate, pageable, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Search and filter attendance records across employees")
    public ResponseEntity<PageResponse<AttendanceResponse>> listAttendance(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "employeeId", required = false) UUID employeeId,
            @RequestParam(name = "status", required = false) AttendanceStatus status,
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(size = 20, sort = "attendanceDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(attendanceService.listAttendance(date, startDate, endDate, employeeId, status, search, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Correct an attendance record", description = "Updates attendance times and records the mandatory correction reason in the audit trail.")
    public ResponseEntity<AttendanceResponse> correctAttendance(
            @PathVariable UUID id,
            @Valid @RequestBody AttendanceCorrectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(attendanceService.correctAttendance(id, request, principal));
    }
}
