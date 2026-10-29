package com.smartattendance.platform.audit.controller;

import com.smartattendance.platform.audit.entity.AuditLog;
import com.smartattendance.platform.audit.service.AuditService;
import com.smartattendance.platform.common.model.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Trail", description = "Endpoints for reviewing administrative and employee activity audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs", description = "Retrieves paginated audit events. Requires ADMIN role.")
    public ResponseEntity<PageResponse<AuditLog>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(auditService.listAuditLogs(pageable)));
    }
}

