# Project Plan: Smart Attendance Platform

## 1. Executive Summary & Project Goals
The **Smart Attendance Platform** is a production-grade, enterprise-ready Employee Attendance Management System built with:
- **Backend**: Java 21, Spring Boot 3.3.x, Spring Security 6 (stateless JWT), Spring Data JPA, Hibernate, Bean Validation, Flyway.
- **Database**: PostgreSQL 16 (Strict ACID compliance, unique constraints, connection pooling via HikariCP).
- **Frontend**: React 18, TypeScript (strict mode), Vite, Tailwind CSS, Lucide icons, Axios / TanStack Query.
- **Testing & Quality**: JUnit 5, Mockito, AssertJ, Spring Boot Test, RFC 9457 Problem Details.
- **DevOps**: Docker, multi-stage Dockerfiles, Docker Compose, GitHub Actions CI.

The system enforces strict business rules around employee shifts, duplicate prevention, and access controls while remaining clean, maintainable, and straightforward to explain during technical interviews.

---

## 2. Core Business Invariants & Edge Cases

### 2.1 Duplicate Clock-In Prevention
- **Rule**: An employee can clock in exactly once per business calendar date.
- **Race Condition Handling**: Concurrency cannot be solved by repository `existsBy...` checks alone. A database-level unique constraint `UNIQUE (employee_id, attendance_date)` ensures that even simultaneous requests are rejected at the persistence boundary. Violations are mapped to an RFC 9457 `409 Conflict` response.

### 2.2 Clock-Out Invariants
- An employee cannot clock out without an active (open) attendance session on that business date.
- An employee cannot clock out if they have already clocked out for the day (`status == COMPLETED`).
- Clock-out timestamp must be strictly greater than or equal to clock-in timestamp (`clock_out_time >= clock_in_time`).

### 2.3 Identity & Time Integrity
- **Client Trust Zero**: The client never supplies their employee ID or server timestamp.
- **Identity**: Derived exclusively from the authenticated JWT security context (`SecurityContextHolder`).
- **Clock**: Derived exclusively from an injectable `java.time.Clock` bean on the server (configured to UTC), allowing deterministic unit and integration testing.

### 2.4 Timezone Policy
- **Storage**: All database timestamps are stored as `TIMESTAMPTZ` normalized to UTC.
- **Business Date Calculation**: The business date is computed by converting the UTC instant to the corporate business timezone (default: `Asia/Kolkata` or `America/New_York`, configurable via `app.business.timezone`).
- **Overnight Shifts**: An attendance record is anchored to the business date on which clock-in occurred. Clock-outs past midnight remain linked to the originating session.

### 2.5 Security & Data Privacy
- Passwords hashed using Spring Security's `BCryptPasswordEncoder` (cost factor 12).
- Sensitive credentials, password hashes, and JWT secrets are never logged or returned in DTOs.
- Dual-key strategy: Internal `BIGINT` identity keys for database performance; non-sequential `UUID` public IDs exposed in REST URLs to prevent Insecure Direct Object Reference (IDOR) attacks.

---

## 3. Phased Implementation Roadmap

```
Phase 1: Backend Foundation (Spring Boot 3.3, Flyway, Docker Compose, RFC 9457)
   │
Phase 2: Employee Domain & Persistence (Entity, Service, Controller, Unit & Integration Tests)
   │
Phase 3: Authentication & Role-Based Access Control (JWT, Spring Security, Dev Seeder)
   │
Phase 4: Core Attendance Engine (Clock-In/Clock-Out, Invariants, Concurrency Safety, Tests)
   │
Phase 5: Advanced Reporting & CSV Exports (Aggregations, Safe CSV Exporter)
   │
Phase 6: Audit Logging, Reliability & API Quality (MDC Tracing, Audit Trail, OpenAPI)
   │
Phase 7: Frontend Dashboard (React + TypeScript + Vite, Responsive UI, Auth Flow)
   │
Phase 8: Docker, CI/CD & Final Verification (Multi-stage Docker, GitHub Actions, README)
   │
Phase 9: Code Review & Interview Preparation Guide (25 Q&A, Edge Case Analysis)
```

---

## 4. Risk Assessment & Mitigations

| Risk / Failure Mode | Impact | Architectural Mitigation |
| :--- | :--- | :--- |
| **Concurrent Clock-in Race Condition** | High | PostgreSQL `UNIQUE(employee_id, attendance_date)` constraint. Service catches `DataIntegrityViolationException` and translates to `409 Conflict`. |
| **IDOR Vulnerabilities** | Critical | Internal surrogate keys (`BIGINT`) are private; external APIs strictly use non-sequential UUIDs (`public_id`). Authorization filters verify ownership. |
| **Timezone Drift & DST Issues** | High | UTC persistence in database; localized business conversion via centralized `BusinessClockService` and injectable `Clock`. |
| **CSV Formula Injection** | High | Cell values starting with `=`, `+`, `-`, or `@` are sanitized by prepending a single quote `'`. |
| **N+1 Database Queries** | Medium | JPA query methods use `JOIN FETCH` / `@EntityGraph` for relationship loading; DTO projection queries for reports. |

