package com.smartattendance.platform.employee.controller;

import com.smartattendance.platform.common.model.PageResponse;
import com.smartattendance.platform.employee.dto.CreateEmployeeRequest;
import com.smartattendance.platform.employee.dto.EmployeeResponse;
import com.smartattendance.platform.employee.dto.UpdateEmployeeRequest;
import com.smartattendance.platform.employee.entity.EmployeeStatus;
import com.smartattendance.platform.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee Management", description = "Endpoints for administering company employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new employee", description = "Creates a new employee record. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Employee created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request input")
    @ApiResponse(responseCode = "409", description = "Employee code or email already exists")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse created = employeeService.createEmployee(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or authentication.principal.publicId == #id")
    @Operation(summary = "Get employee by ID", description = "Retrieves employee profile by public UUID.")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(employeeService.getEmployeeByPublicId(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List employees with pagination and filters")
    public ResponseEntity<PageResponse<EmployeeResponse>> listEmployees(
            @RequestParam(name = "status", required = false) EmployeeStatus status,
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(employeeService.listEmployees(status, search, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update employee details", description = "Updates employee metadata. Requires ADMIN role.")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate employee", description = "Sets employee status to INACTIVE. Requires ADMIN role.")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(employeeService.deactivateEmployee(id));
    }
}

