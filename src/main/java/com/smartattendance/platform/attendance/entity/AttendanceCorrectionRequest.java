package com.smartattendance.platform.attendance.entity;

import com.smartattendance.platform.employee.entity.Employee;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_correction_requests")
public class AttendanceCorrectionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private AttendanceRecord attendance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @Column(name = "proposed_clock_in", nullable = false)
    private Instant proposedClockIn;

    @Column(name = "proposed_clock_out")
    private Instant proposedClockOut;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CorrectionRequestStatus status = CorrectionRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Employee reviewer;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AttendanceCorrectionRequest() {}

    public AttendanceCorrectionRequest(UUID publicId, AttendanceRecord attendance, Employee requester,
                                       Instant proposedClockIn, Instant proposedClockOut, String reason) {
        this.publicId = publicId == null ? UUID.randomUUID() : publicId;
        this.attendance = attendance;
        this.requester = requester;
        this.proposedClockIn = proposedClockIn;
        this.proposedClockOut = proposedClockOut;
        this.reason = reason;
    }

    @PrePersist
    void prePersist() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public AttendanceRecord getAttendance() { return attendance; }
    public Employee getRequester() { return requester; }
    public Instant getProposedClockIn() { return proposedClockIn; }
    public Instant getProposedClockOut() { return proposedClockOut; }
    public String getReason() { return reason; }
    public CorrectionRequestStatus getStatus() { return status; }
    public Employee getReviewer() { return reviewer; }
    public String getDecisionReason() { return decisionReason; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void approve(Employee reviewer, String decisionReason) {
        this.status = CorrectionRequestStatus.APPROVED;
        this.reviewer = reviewer;
        this.decisionReason = decisionReason;
        this.reviewedAt = Instant.now();
    }
    public void reject(Employee reviewer, String decisionReason) {
        this.status = CorrectionRequestStatus.REJECTED;
        this.reviewer = reviewer;
        this.decisionReason = decisionReason;
        this.reviewedAt = Instant.now();
    }
}
