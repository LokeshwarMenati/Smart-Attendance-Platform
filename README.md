# Smart Attendance Platform

> Production-grade, enterprise-ready Employee Attendance Management System built with **Java 21**, **Spring Boot 3.3**, **PostgreSQL 16**, **Flyway**, and **React 18 + TypeScript + Vite**.

---

## 1. System Architecture

- **Backend**: Java 21 LTS, Spring Boot 3.3.5, Spring Security 6 (stateless JWT), Spring Data JPA, Hibernate, Bean Validation, HikariCP, Flyway, Actuator.
- **Database**: PostgreSQL 16 with strict relational constraints, conditional unique indices, and dual-key design (`BIGINT` PK + `UUID` public ID).
- **Frontend**: React 18, TypeScript (strict mode), Vite, Lucide Icons, Axios with JWT interceptors.
- **Security**: BCrypt password hashing (work factor 12), stateless signed JWT tokens (HMAC-SHA256), OWASP CSV formula injection defense, RFC 9457 Problem Details error handling.
- **Testing**: JUnit 5, Mockito, AssertJ, Spring Boot Test (42 automated tests passing).

---

## 2. Quick Start

### 2.1 Start with Docker Compose (Recommended)
```bash
docker compose up -d
```
This spins up:
- **PostgreSQL 16** on port `5432`
- **Spring Boot Backend** on port `8080`
- **React Frontend** on port `3000`

### 2.2 Local Development Startup (Without Docker)

#### Step 1: Start PostgreSQL
```bash
docker compose up -d postgres
```

#### Step 2: Build & Start Backend
```bash
# Windows
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

# Linux / macOS
./mvnw clean package
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
The backend starts at `http://localhost:8080`.

#### Step 3: Start Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend starts at `http://localhost:3000`.

---

## 3. Demo Test Accounts

The platform automatically seeds initial demo accounts when started in local/dev profiles:

| Role | Email | Password | Code |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@smartattendance.com` | `Admin@12345` | `ADM-001` |
| **MANAGER** | `manager@smartattendance.com` | `Manager@12345` | `MGR-001` |
| **EMPLOYEE** | `employee@smartattendance.com` | `Employee@12345` | `EMP-001` |

---

## 4. Key Endpoints & cURL Examples

### 4.1 Authenticate & Obtain JWT
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "employee@smartattendance.com", "password": "Employee@12345"}'
```
Response:
```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresInSeconds": 86400,
  "employee": {
    "employeeCode": "EMP-001",
    "fullName": "Alex Rivera",
    "role": "EMPLOYEE"
  }
}
```

### 4.2 Clock In
```bash
curl -X POST http://localhost:8080/api/v1/attendance/clock-in \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"notes": "Starting morning shift"}'
```

### 4.3 Clock Out
```bash
curl -X POST http://localhost:8080/api/v1/attendance/clock-out \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"notes": "Completed daily shift"}'
```

### 4.4 View Personal Attendance History
```bash
curl -X GET "http://localhost:8080/api/v1/attendance/me?page=0&size=10" \
  -H "Authorization: Bearer <TOKEN>"
```

### 4.5 Export Attendance CSV (Admin/Manager)
```bash
curl -X GET "http://localhost:8080/api/v1/reports/attendance/export" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -o attendance_export.csv
```

---

## 5. Running Automated Tests

```bash
# Run all backend unit and integration tests (42 tests)
.\mvnw.cmd test

# Run frontend production build & typecheck
cd frontend
npm run build
```

---

## 6. Interactive Documentation & Observability

- **Swagger UI Interactive API**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI 3 JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 7. Documentation Index

- [`docs/PROJECT_PLAN.md`](file:///docs/PROJECT_PLAN.md) — Phased architecture roadmap & risk mitigations.
- [`docs/ARCHITECTURE.md`](file:///docs/ARCHITECTURE.md) — Modular monolith design & ADRs.
- [`docs/API_SPECIFICATION.md`](file:///docs/API_SPECIFICATION.md) — REST API specification & RFC 9457 error contracts.
- [`docs/DATABASE_DESIGN.md`](file:///docs/DATABASE_DESIGN.md) — PostgreSQL ERD, constraints, and Flyway migration plan.
- [`docs/TESTING.md`](file:///docs/TESTING.md) — Test execution guide and coverage overview.
- [`docs/INTERVIEW_GUIDE.md`](file:///docs/INTERVIEW_GUIDE.md) — Comprehensive technical interview guide with 25 questions & answers.
