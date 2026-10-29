package com.smartattendance.platform.notification.entity;

import com.smartattendance.platform.employee.entity.Employee;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Employee recipient;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "type", nullable = false, length = 40)
    private String type;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {}

    public Notification(Employee recipient, String title, String message, String link, String type) {
        this.publicId = UUID.randomUUID();
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.link = link;
        this.type = type;
    }

    @PrePersist
    void prePersist() {
        if (publicId == null) publicId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getPublicId() { return publicId; }
    public Employee getRecipient() { return recipient; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getLink() { return link; }
    public String getType() { return type; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void markRead() { this.readAt = Instant.now(); }
}
