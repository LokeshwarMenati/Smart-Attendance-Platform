package com.smartattendance.platform.attendance.dto;

import com.smartattendance.platform.attendance.entity.AttendanceCorrectionRequest;
import com.smartattendance.platform.attendance.entity.CorrectionRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record CorrectionRequestResponse(
        UUID id, UUID attendanceId, UUID requesterId, String requesterName,
        Instant proposedClockIn, Instant proposedClockOut, String reason,
        CorrectionRequestStatus status, String reviewerName, String decisionReason,
        Instant reviewedAt, Instant createdAt
) {
    public static CorrectionRequestResponse from(AttendanceCorrectionRequest request) {
        return new CorrectionRequestResponse(
                request.getPublicId(), request.getAttendance().getPublicId(),
                request.getRequester().getPublicId(), request.getRequester().getFullName(),
                request.getProposedClockIn(), request.getProposedClockOut(), request.getReason(),
                request.getStatus(),
                request.getReviewer() == null ? null : request.getReviewer().getFullName(),
                request.getDecisionReason(), request.getReviewedAt(), request.getCreatedAt()
        );
    }
}
