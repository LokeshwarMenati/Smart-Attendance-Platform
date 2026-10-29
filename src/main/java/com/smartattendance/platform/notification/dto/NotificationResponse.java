package com.smartattendance.platform.notification.dto;

import com.smartattendance.platform.notification.entity.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id, String title, String message, String link, String type,
        boolean read, Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getPublicId(), notification.getTitle(),
                notification.getMessage(), notification.getLink(), notification.getType(),
                notification.getReadAt() != null, notification.getCreatedAt());
    }
}
