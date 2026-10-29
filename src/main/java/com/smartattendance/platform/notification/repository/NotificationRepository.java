package com.smartattendance.platform.notification.repository;

import com.smartattendance.platform.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
        SELECT n FROM Notification n
        WHERE n.recipient.publicId = :recipientId
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findForRecipient(@Param("recipientId") UUID recipientId, Pageable pageable);

    long countByRecipientPublicIdAndReadAtIsNull(UUID recipientId);

    Optional<Notification> findByPublicIdAndRecipientPublicId(UUID id, UUID recipientId);
}
