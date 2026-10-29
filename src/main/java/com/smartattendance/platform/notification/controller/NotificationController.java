package com.smartattendance.platform.notification.controller;

import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.notification.dto.NotificationResponse;
import com.smartattendance.platform.notification.service.NotificationService;
import com.smartattendance.platform.security.model.UserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.list(principal, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.unreadCount(principal));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal);
        return ResponseEntity.noContent().build();
    }
}
