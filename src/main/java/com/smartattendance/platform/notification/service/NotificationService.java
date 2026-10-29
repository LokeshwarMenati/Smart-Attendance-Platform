package com.smartattendance.platform.notification.service;

import com.smartattendance.platform.common.exception.ResourceNotFoundException;
import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.employee.entity.Employee;
import com.smartattendance.platform.employee.entity.EmployeeRole;
import com.smartattendance.platform.employee.repository.EmployeeRepository;
import com.smartattendance.platform.notification.dto.NotificationResponse;
import com.smartattendance.platform.notification.entity.Notification;
import com.smartattendance.platform.notification.repository.NotificationRepository;
import com.smartattendance.platform.security.model.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

    public NotificationService(NotificationRepository notificationRepository, EmployeeRepository employeeRepository) {
        this.notificationRepository = notificationRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UserPrincipal principal, Pageable pageable) {
        Page<Notification> page = notificationRepository.findForRecipient(principal.getPublicId(), pageable);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UserPrincipal principal) {
        return notificationRepository.countByRecipientPublicIdAndReadAtIsNull(principal.getPublicId());
    }

    @Transactional
    public void markRead(UUID id, UserPrincipal principal) {
        Notification notification = notificationRepository.findByPublicIdAndRecipientPublicId(id, principal.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification was not found."));
        notification.markRead();
    }

    @Transactional
    public void notifyRole(EmployeeRole role, String title, String message, String link, String type) {
        employeeRepository.findByRole(role).forEach(employee ->
                notificationRepository.save(new Notification(employee, title, message, link, type)));
    }

    @Transactional
    public void notifyEmployee(Employee employee, String title, String message, String link, String type) {
        notificationRepository.save(new Notification(employee, title, message, link, type));
    }
}
