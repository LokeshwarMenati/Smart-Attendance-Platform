package com.smartattendance.platform.attendance.controller;

import com.smartattendance.platform.attendance.dto.CorrectionDecisionRequest;
import com.smartattendance.platform.attendance.dto.CorrectionRequestCreate;
import com.smartattendance.platform.attendance.dto.CorrectionRequestResponse;
import com.smartattendance.platform.attendance.entity.CorrectionRequestStatus;
import com.smartattendance.platform.attendance.service.AttendanceCorrectionService;
import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.security.model.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance/correction-requests")
public class AttendanceCorrectionController {
    private final AttendanceCorrectionService service;

    public AttendanceCorrectionController(AttendanceCorrectionService service) {
        this.service = service;
    }

    @PostMapping("/for/{attendanceId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<CorrectionRequestResponse> submit(
            @PathVariable UUID attendanceId,
            @Valid @RequestBody CorrectionRequestCreate request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(attendanceId, request, principal));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CorrectionRequestResponse>> list(
            @RequestParam(required = false) CorrectionRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.list(status, pageable, principal));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CorrectionRequestResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody CorrectionDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.decide(id, true, request, principal));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<CorrectionRequestResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody CorrectionDecisionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.decide(id, false, request, principal));
    }
}
