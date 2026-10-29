package com.smartattendance.platform.audit.event;

import java.util.UUID;

public record AuditEvent(
        UUID actorId,
        String actorEmail,
        String action,
        String targetResource,
        String targetId,
        String metadata
) {}

