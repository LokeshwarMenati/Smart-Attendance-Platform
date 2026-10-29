package com.smartattendance.platform.attendance.service;

import com.smartattendance.platform.attendance.dto.CorrectionDecisionRequest;
import com.smartattendance.platform.attendance.dto.CorrectionRequestCreate;
import com.smartattendance.platform.attendance.dto.CorrectionRequestResponse;
import com.smartattendance.platform.attendance.entity.AttendanceCorrectionRequest;
import com.smartattendance.platform.attendance.entity.AttendanceRecord;
import com.smartattendance.platform.attendance.entity.AttendanceStatus;
import com.smartattendance.platform.attendance.entity.CorrectionRequestStatus;
import com.smartattendance.platform.attendance.repository.AttendanceCorrectionRequestRepository;
import com.smartattendance.platform.attendance.repository.AttendanceRepository;
import com.smartattendance.platform.audit.event.AuditEvent;
import com.smartattendance.platform.common.exception.DuplicateResourceException;
import com.smartattendance.platform.common.exception.InvalidOperationException;
import com.smartattendance.platform.common.exception.ResourceNotFoundException;
import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.security.model.UserPrincipal;
import com.smartattendance.platform.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AttendanceCorrectionService {
    private final AttendanceCorrectionRequestRepository requestRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    public AttendanceCorrectionService(AttendanceCorrectionRequestRepository requestRepository,
                                       AttendanceRepository attendanceRepository,
                                       EmployeeRepository employeeRepository,
                                       org.springframework.context.ApplicationEventPublisher eventPublisher,
                                       NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    @Transactional
    public CorrectionRequestResponse submit(UUID attendanceId, CorrectionRequestCreate input, UserPrincipal principal) {
        AttendanceRecord attendance = attendanceRepository.findByPublicIdWithEmployee(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record was not found."));
        if (!attendance.getEmployee().getPublicId().equals(principal.getPublicId())) {
            throw new AccessDeniedException("Employees can only request corrections for their own attendance.");
        }
        if (input.proposedClockOut() != null && input.proposedClockOut().isBefore(input.proposedClockIn())) {
            throw new InvalidOperationException("Proposed clock-out cannot be earlier than proposed clock-in.");
        }
        if (requestRepository.existsByAttendanceIdAndStatus(attendance.getId(), CorrectionRequestStatus.PENDING)) {
            throw new DuplicateResourceException("A correction request is already pending for this attendance record.");
        }
        Employee requester = employeeRepository.findByPublicId(principal.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Requester was not found."));
        AttendanceCorrectionRequest request = requestRepository.save(new AttendanceCorrectionRequest(
                null, attendance, requester, input.proposedClockIn(), input.proposedClockOut(), input.reason().trim()
        ));
        eventPublisher.publishEvent(new AuditEvent(principal.getPublicId(), principal.getUsername(),
                "CORRECTION_REQUEST_SUBMITTED", "ATTENDANCE_CORRECTION", request.getPublicId().toString(),
                "reason=" + input.reason().trim()));
        notificationService.notifyRole(EmployeeRole.MANAGER, "New correction request",
                requester.getFullName() + " submitted an attendance correction request.",
                "/corrections", "CORRECTION_REQUEST");
        notificationService.notifyRole(EmployeeRole.ADMIN, "New correction request",
                requester.getFullName() + " submitted an attendance correction request.",
                "/corrections", "CORRECTION_REQUEST");
        return CorrectionRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<CorrectionRequestResponse> list(CorrectionRequestStatus status,
                                                         Pageable pageable, UserPrincipal principal) {
        boolean privileged = principal.getRole() == EmployeeRole.ADMIN || principal.getRole() == EmployeeRole.MANAGER;
        Page<AttendanceCorrectionRequest> page = requestRepository.findAllForScope(
                privileged ? null : principal.getPublicId(), status, pageable);
        return PageResponse.from(page.map(CorrectionRequestResponse::from));
    }

    @Transactional
    public CorrectionRequestResponse decide(UUID requestId, boolean approve,
                                            CorrectionDecisionRequest input, UserPrincipal principal) {
        if (principal.getRole() != EmployeeRole.ADMIN && principal.getRole() != EmployeeRole.MANAGER) {
            throw new AccessDeniedException("Only managers and administrators can review correction requests.");
        }
        AttendanceCorrectionRequest request = requestRepository.findByPublicId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Correction request was not found."));
        if (request.getStatus() != CorrectionRequestStatus.PENDING) {
            throw new InvalidOperationException("Only pending correction requests can be reviewed.");
        }
        Employee reviewer = employeeRepository.findByPublicId(principal.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer was not found."));
        if (approve) {
            AttendanceRecord record = request.getAttendance();
            Instant oldIn = record.getClockInTime();
            Instant oldOut = record.getClockOutTime();
            record.setClockInTime(request.getProposedClockIn());
            record.setClockOutTime(request.getProposedClockOut());
            record.setDurationMinutes(request.getProposedClockOut() == null ? null :
                    Math.toIntExact(Duration.between(request.getProposedClockIn(), request.getProposedClockOut()).toMinutes()));
            record.setStatus(request.getProposedClockOut() == null ? AttendanceStatus.OPEN : AttendanceStatus.COMPLETED);
            attendanceRepository.save(record);
            request.approve(reviewer, input.reason().trim());
            eventPublisher.publishEvent(new AuditEvent(principal.getPublicId(), principal.getUsername(),
                    "CORRECTION_REQUEST_APPROVED", "ATTENDANCE", record.getPublicId().toString(),
                    "request=" + request.getPublicId() + ";oldIn=" + oldIn + ";oldOut=" + oldOut
                            + ";newIn=" + record.getClockInTime() + ";newOut=" + record.getClockOutTime()
                            + ";reason=" + input.reason().trim()));
            notificationService.notifyEmployee(request.getRequester(), "Correction request approved",
                    "Your attendance correction request was approved.", "/corrections", "CORRECTION_DECISION");
        } else {
            request.reject(reviewer, input.reason().trim());
            eventPublisher.publishEvent(new AuditEvent(principal.getPublicId(), principal.getUsername(),
                    "CORRECTION_REQUEST_REJECTED", "ATTENDANCE_CORRECTION", request.getPublicId().toString(),
                    "reason=" + input.reason().trim()));
            notificationService.notifyEmployee(request.getRequester(), "Correction request rejected",
                    "Your attendance correction request was rejected.", "/corrections", "CORRECTION_DECISION");
        }
        return CorrectionRequestResponse.from(requestRepository.save(request));
    }
}
