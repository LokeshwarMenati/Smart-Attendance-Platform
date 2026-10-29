# Smart Attendance Platform

> Production-grade, enterprise-ready Employee Attendance Management System built with **Java 21**, **Spring Boot 3.3**, **PostgreSQL 16**, **Flyway**, and **React 18 + TypeScript + Vite**.

---

## 🔗 Live Application Links

| Resource | URL | Description |
|---|---|---|
| 🚀 **Live Web App (Vercel)** | **[https://frontend-uer5.vercel.app/](https://frontend-uer5.vercel.app/)** | Public production deployment on Vercel |
| 🌐 **Alternative Live URL** | **[https://frontend-sand-eight-xya4czd8dy.vercel.app/](https://frontend-sand-eight-xya4czd8dy.vercel.app/)** | Direct deployment URL |
| 💻 **Local Development App** | **[http://localhost:3000/](http://localhost:3000/)** | Local Vite development server |
| 📚 **Swagger UI / API Docs** | **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)** | Interactive OpenAPI test console |
| 🩺 **Backend Health Check** | **[http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)** | Spring Boot Actuator health endpoint |

### 🔑 Demo Accounts (Pre-seeded & 1-Click Available)

| Role | Email | Password | Permissions |
|---|---|---|---|
| **Employee** | `employee@smartattendance.com` | `Employee@12345` | Clock-in, clock-out, view personal history, request corrections |
| **Manager** | `manager@smartattendance.com` | `Manager@12345` | Team overview, approve/reject correction requests |
| **Admin** | `admin@smartattendance.com` | `Admin@12345` | Employee CRUD, system configuration, audit logs, CSV exports |

---

## 1. System Architecture

- **Backend**: Java 21 LTS, Spring Boot 3.3.5, Spring Security 6 (stateless JWT), Spring Data JPA, Hibernate, Bean Validation, HikariCP, Flyway, Actuator.
- **Database**: PostgreSQL 16 with strict relational constraints, conditional unique indices, and dual-key design (`BIGINT` PK + `UUID` public ID).
- **Frontend**: React 18, TypeScript (strict mode), Vite, Lucide Icons, Axios with JWT interceptors.
- **Security**: BCrypt password hashing (work factor 12), stateless signed JWT tokens (HMAC-SHA256), OWASP CSV formula injection defense, RFC 9457 Problem Details error handling.
- **Testing**: JUnit 5, Mockito, AssertJ, Spring Boot Test (42 automated tests passing).

---

## 2. Quick Start

These steps can be followed directly after cloning this repository from GitHub.

### 2.1 Prerequisites

Install the following tools:

- Git
- Java 21 JDK
- Node.js 18 or newer and npm
- Docker Desktop (recommended) or PostgreSQL 16

Verify the installations:

```bash
git --version
java -version
node --version
npm --version
docker --version
```

### 2.2 Clone the Repository

```bash
git clone https://github.com/LokeshwarMenati/Smart-Attendance-Platform.git
cd Smart-Attendance-Platform
```

### 2.3 Start with Docker Compose (Recommended)

From the project root, run:

```bash
docker compose up -d
```

This spins up:
- **PostgreSQL 16** on port `5432`
- **Spring Boot Backend** on port `8080`
- **React Frontend** on port `3000`

Open the application:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

To stop the containers:

```bash
docker compose down
```

### 2.4 Local Development Startup (Without Docker)

Use this option when PostgreSQL is already installed locally.

#### Step 1: Configure the database

Create a PostgreSQL database named `smart_attendance`, then set the required environment variables from `.env.example`:

```bash
DB_URL=jdbc:postgresql://localhost:5432/smart_attendance
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password
JWT_SECRET=replace_with_a_secure_32_byte_secret
```

#### Step 2: Start PostgreSQL

If PostgreSQL is running through Docker:

```bash
docker compose up -d postgres
```

#### Step 3: Build & Start Backend

Windows PowerShell:

```bash
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Linux / macOS:

```bash
./mvnw clean package
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The backend starts at `http://localhost:8080`.

#### Step 4: Start Frontend

Open a second terminal at the project root:

```bash
cd frontend/
npm install
npm run dev
```

The frontend starts at `http://localhost:3000`.

#### Step 5: Verify the application

Open [http://localhost:3000](http://localhost:3000), then sign in with one of the demo accounts below. Confirm that the backend is available at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

### 2.5 Common Run Commands

```bash
# Start backend with the local H2 profile (Windows)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"

# Start frontend
cd frontend
npm run dev

# Stop Docker services
docker compose down
```

### 2.6 Deploying the Frontend to Vercel

The Vercel frontend must point to a publicly deployed Spring Boot backend. In the Vercel project settings, add this environment variable for **Production** and **Preview**:

```text
VITE_API_URL=https://your-backend-domain.example.com
```

Use the backend origin only; do not append `/api/v1`. The application adds `/api/v1` automatically. Redeploy the frontend after saving the variable.

On the backend hosting platform, configure CORS to allow the Vercel origin:

```text
CORS_ALLOWED_ORIGINS=https://frontend-uer5.vercel.app
```

If you use more than one frontend origin, separate them with commas.

For local development, leave `VITE_API_URL` unset. Vite then uses its local proxy at `http://localhost:8080`.

---

## 3. Demo Test Accounts

The platform automatically seeds initial demo accounts when started in local/dev profiles:

| Role | Email | Password | Code |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@smartattendance.com` | `Admin@12345` | `ADM-001` |
| **MANAGER** | `manager@smartattendance.com` | `Manager@12345` | `MGR-001` |
| **EMPLOYEE** | `employee@smartattendance.com` | `Employee@12345` | `EMP-001` |

The local profile also creates additional demo employees across the Morning, Evening, and Night shifts so managers and administrators can review attendance analytics.

## 4. Skills Used

This project demonstrates the following technical skills:

- Java 21 and object-oriented backend development
- Spring Boot REST API development
- Spring Security, JWT authentication, and role-based access control
- Spring Data JPA and Hibernate entity/repository design
- PostgreSQL database design, constraints, transactions, and Flyway migrations
- H2 local development profile and idempotent demo-data seeding
- Attendance domain modeling, clock-in/clock-out workflows, duration calculation, and overnight-session handling
- Attendance correction requests with manager/admin approval and rejection
- Attendance analytics, exception monitoring, audit trails, and notifications
- React 18, TypeScript, Vite, and responsive CSS
- Axios API integration and protected frontend routes
- JUnit 5, Mockito, AssertJ, and Spring Boot integration testing
- Docker Compose, Maven Wrapper, GitHub Actions, and API documentation with OpenAPI
- Timezone-aware timestamps, validation, concurrency-safe business rules, and error handling

---

## 5. Key Endpoints & cURL Examples

### 5.1 Authenticate & Obtain JWT
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

### 5.2 Clock In
```bash
curl -X POST http://localhost:8080/api/v1/attendance/clock-in \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"notes": "Starting morning shift"}'
```

### 5.3 Clock Out
```bash
curl -X POST http://localhost:8080/api/v1/attendance/clock-out \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"notes": "Completed daily shift"}'
```

### 5.4 View Personal Attendance History
```bash
curl -X GET "http://localhost:8080/api/v1/attendance/me?page=0&size=10" \
  -H "Authorization: Bearer <TOKEN>"
```

### 5.5 Export Attendance CSV (Admin/Manager)
```bash
curl -X GET "http://localhost:8080/api/v1/reports/attendance/export" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -o attendance_export.csv
```

---

## 6. Running Automated Tests

```bash
# Run all backend unit and integration tests (42 tests)
.\mvnw.cmd test

# Run frontend production build & typecheck
cd frontend
npm run build
```

---

## 7. Interactive Documentation & Observability

- **Swagger UI Interactive API**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI 3 JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 8. Documentation Index

- [`docs/PROJECT_PLAN.md`](docs/PROJECT_PLAN.md) — Phased architecture roadmap & risk mitigations.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — Modular monolith design & ADRs.
- [`docs/API_SPECIFICATION.md`](docs/API_SPECIFICATION.md) — REST API specification & RFC 9457 error contracts.
- [`docs/DATABASE_DESIGN.md`](docs/DATABASE_DESIGN.md) — PostgreSQL ERD, constraints, and Flyway migration plan.
- [`docs/TESTING.md`](docs/TESTING.md) — Test execution guide and coverage overview.
- [`docs/INTERVIEW_GUIDE.md`](docs/INTERVIEW_GUIDE.md) — Comprehensive technical interview guide with 25 questions & answers.
