# Testing Architecture & Verification Guide

This document describes the testing strategy, test layers, and verification commands for the **Smart Attendance Platform**.

---

## 1. Test Architecture Overview

```
                        ┌──────────────────────────────┐
                        │   End-to-End / Web Tests     │
                        │   (Frontend UI & MockMvc)    │
                        └──────────────┬───────────────┘
                                       │
                        ┌──────────────▼──────────────┐
                        │   Integration Test Layer    │
                        │ (Controllers, Security, DB) │
                        └──────────────┬───────────────┘
                                       │
                        ┌──────────────▼──────────────┐
                        │      Unit Test Layer        │
                        │ (Domain Services, Mocks)    │
                        └──────────────────────────────┘
```

The test suite contains **42 automated tests** covering:
- **Application Context & Configuration**: Health checks, actuator metrics, JVM UTC configuration.
- **RFC 9457 Global Error Handling**: 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict.
- **Employee Domain**: CRUD operations, soft-deactivation, unique email/code constraints, password hashing.
- **Security & Authorization**: JWT generation, expiration, malformed token rejection, role-based access control (EMPLOYEE, MANAGER, ADMIN), and cross-employee access isolation.
- **Attendance Business Engine**: Clock-in, clock-out, duplicate clock-in rejection (409 Conflict), open session verification, duration calculations.
- **Reports & Analytics**: Daily attendance aggregations, range summaries, CSV formula injection protection (`=`, `+`, `-`, `@`).
- **Audit Logging**: Automatic audit log creation on domain events.

---

## 2. Test Execution Commands

### 2.1 Run All Backend Tests
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 2.2 Run Specific Test Classes
```bash
# Run Attendance tests only
.\mvnw.cmd test -Dtest=AttendanceControllerTest,AttendanceServiceTest

# Run Security & Authorization tests only
.\mvnw.cmd test -Dtest=AuthControllerTest,AuthorizationRbacTest

# Run Reporting & CSV tests only
.\mvnw.cmd test -Dtest=ReportingControllerTest
```

### 2.3 Run Frontend Production Build & Typecheck
```bash
cd frontend
npm run build
```

---

## 3. Key Test Cases Explained

| Test Class | Method | Scenario Tested | Expected Invariant |
| :--- | :--- | :--- | :--- |
| `AttendanceControllerTest` | `duplicateClockInRejected` | Employee tries to clock in twice on the same day | HTTP 409 Conflict returned; second record denied |
| `AttendanceControllerTest` | `clockOutWithoutClockInFails` | Employee attempts to clock out without an open session | HTTP 400 Bad Request with RFC 9457 Problem Details |
| `AuthorizationRbacTest` | `employeeCannotAccessOtherEmployee` | Employee attempts to access another employee's profile via URL ID | HTTP 403 Forbidden |
| `ReportingControllerTest` | `exportAttendanceCsvWithSanitization` | Notes field contains formula `=SUM(A1:A10)` | Cell sanitized to `'=SUM(A1:A10)` preventing CSV injection |

