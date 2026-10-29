package com.smartattendance.platform.audit.service;

import com.smartattendance.platform.audit.entity.AuditLog;
import com.smartattendance.platform.audit.event.AuditEvent;
import com.smartattendance.platform.audit.repository.AuditLogRepository;
import com.smartattendance.platform.common.config.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(AuditEvent event) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);

        AuditLog auditLog = new AuditLog(
                event.actorId(),
                event.actorEmail(),
                event.action(),
                event.targetResource(),
                event.targetId(),
                null, // IP can be captured via filter if available
                correlationId,
                event.metadata()
        );

        auditLogRepository.save(auditLog);
        log.info("Audit logged: action={}, resource={}, targetId={}, actor={}",
                event.action(), event.targetResource(), event.targetId(), event.actorEmail());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}

