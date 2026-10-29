package com.smartattendance.platform.attendance.entity;

import com.smartattendance.platform.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "attendances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_attendances_employee_date", columnNames = {"employee_id", "attendance_date"}),
                @UniqueConstraint(name = "uq_attendances_public_id", columnNames = {"public_id"})
        }
)
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "clock_in_time", nullable = false)
    private Instant clockInTime;

    @Column(name = "clock_out_time")
    private Instant clockOutTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AttendanceRecord() {
        // Default constructor for JPA
    }

    public AttendanceRecord(UUID publicId,
                            Employee employee,
                            LocalDate attendanceDate,
                            Instant clockInTime,
                            String notes) {
        this.publicId = publicId != null ? publicId : UUID.randomUUID();
        this.employee = employee;
        this.attendanceDate = attendanceDate;
        this.clockInTime = clockInTime;
        this.status = AttendanceStatus.OPEN;
        this.notes = notes;
    }

    @PrePersist
    public void prePersist() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Instant getClockInTime() {
        return clockInTime;
    }

    public void setClockInTime(Instant clockInTime) {
        this.clockInTime = clockInTime;
    }

    public Instant getClockOutTime() {
        return clockOutTime;
    }

    public void setClockOutTime(Instant clockOutTime) {
        this.clockOutTime = clockOutTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttendanceRecord that)) return false;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}

