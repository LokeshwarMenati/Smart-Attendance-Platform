package com.smartattendance.platform.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "target_resource", nullable = false, length = 50)
    private String targetResource;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {
        // JPA standard default constructor
    }

    public AuditLog(UUID actorId,
                    String actorEmail,
                    String action,
                    String targetResource,
                    String targetId,
                    String ipAddress,
                    String correlationId,
                    String metadata) {
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetResource = targetResource;
        this.targetId = targetId;
        this.ipAddress = ipAddress;
        this.correlationId = correlationId;
        this.metadata = metadata;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getTargetResource() {
        return targetResource;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

