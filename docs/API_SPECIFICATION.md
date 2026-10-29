# REST API Specification: Smart Attendance Platform

**Base URL**: `/api/v1`  
**Authentication**: Bearer Token via `Authorization: Bearer <JWT>`  
**Content-Type**: `application/json`  
**Error Standard**: RFC 9457 Problem Details (`application/problem+json`)

---

## 1. Authentication Endpoints

### 1.1 POST `/api/v1/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "alex.dev@smartattendance.com",
    "password": "Password123!"
  }
  ```
- **Responses**:
  - `200 OK`:
    ```json
    {
      "tokenType": "Bearer",
      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
      "expiresInSeconds": 86400,
      "employee": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "employeeCode": "EMP-001",
        "firstName": "Alex",
        "lastName": "Rivera",
        "email": "alex.dev@smartattendance.com",
        "role": "EMPLOYEE"
      }
    }
    ```
  - `401 Unauthorized`: Bad credentials or inactive account.

### 1.2 GET `/api/v1/auth/me`
- **Access**: Authenticated (Any role)
- **Responses**:
  - `200 OK`: Current authenticated employee profile (`EmployeeResponse`).

---

## 2. Employee Management Endpoints

### 2.1 POST `/api/v1/employees`
- **Access**: `ADMIN` only
- **Request Body**:
  ```json
  {
    "employeeCode": "EMP-042",
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@smartattendance.com",
    "password": "SecurePassword123!",
    "role": "EMPLOYEE"
  }
  ```
- **Responses**:
  - `201 Created` with `Location: /api/v1/employees/{id}`
  - `400 Bad Request`: Validation failure.
  - `409 Conflict`: Duplicate employee code or email.

### 2.2 GET `/api/v1/employees`
- **Access**: `ADMIN`, `MANAGER`
- **Query Params**: `page` (default 0), `size` (default 20), `status` (ACTIVE/INACTIVE), `search` (keyword)
- **Response**: `200 OK` (Paginated `PageResponse<EmployeeResponse>`).

### 2.3 GET `/api/v1/employees/{id}`
- **Access**: `ADMIN`, `MANAGER`, or `EMPLOYEE` (self only).
- **Responses**: `200 OK`, `403 Forbidden`, `404 Not Found`.

### 2.4 PUT `/api/v1/employees/{id}`
- **Access**: `ADMIN` only.
- **Request Body**: `UpdateEmployeeRequest` (`firstName`, `lastName`, `role`, `status`).

### 2.5 PATCH `/api/v1/employees/{id}/deactivate`
- **Access**: `ADMIN` only. Soft-deactivates the employee account.

---

## 3. Attendance Engine Endpoints

### 3.1 POST `/api/v1/attendance/clock-in`
- **Access**: Authenticated `EMPLOYEE` (clocks in self).
- **Request Body**:
  ```json
  {
    "notes": "Clocking in from main office"
  }
  ```
- **Responses**:
  - `201 Created`:
    ```json
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "employeeId": "550e8400-e29b-41d4-a716-446655440000",
      "employeeCode": "EMP-001",
      "employeeName": "Alex Rivera",
      "attendanceDate": "2026-10-28",
      "clockInTime": "2026-10-28T09:00:15Z",
      "clockOutTime": null,
      "durationMinutes": null,
      "status": "OPEN",
      "notes": "Clocking in from main office"
    }
    ```
  - `409 Conflict`: If an attendance record for this employee on this business date already exists.
  - `403 Forbidden`: Inactive employee account.

### 3.2 POST `/api/v1/attendance/clock-out`
- **Access**: Authenticated `EMPLOYEE`.
- **Request Body**:
  ```json
  {
    "notes": "Completed daily shift"
  }
  ```
- **Responses**:
  - `200 OK`: Updated attendance record with `clockOutTime`, computed `durationMinutes`, and status `COMPLETED`.
  - `400 Bad Request`: If no open session exists for the employee.
  - `409 Conflict`: If already clocked out.

### 3.3 GET `/api/v1/attendance/me`
- **Access**: Authenticated `EMPLOYEE`.
- **Query Params**: `startDate`, `endDate`, `page`, `size`, `sort`.
- **Response**: `200 OK` (Personal attendance history).

### 3.4 GET `/api/v1/attendance/{id}`
- **Access**: `ADMIN`, `MANAGER`, or record owner.

### 3.5 GET `/api/v1/attendance`
- **Access**: `ADMIN`, `MANAGER`.
- **Query Params**: `date`, `startDate`, `endDate`, `employeeId`, `status`, `page`, `size`, `sort`.

---

## 4. Reporting & CSV Export Endpoints

### 4.1 GET `/api/v1/reports/attendance/daily?date=2026-10-28`
- **Access**: `ADMIN`, `MANAGER`.
- **Response**: Breakdown of employees present, absent, on open shifts, and completed shifts for the date.

### 4.2 GET `/api/v1/reports/attendance/summary?startDate=2026-10-01&endDate=2026-10-28`
- **Access**: `ADMIN`, `MANAGER` (or `EMPLOYEE` scoped to self).
- **Responses**: Aggregated metrics: total sessions, completed, open, total duration minutes, average minutes per session.

### 4.3 GET `/api/v1/reports/attendance/export?startDate=...&endDate=...`
- **Access**: `ADMIN`, `MANAGER` (or `EMPLOYEE` scoped to self).
- **Produces**: `text/csv; charset=UTF-8`
- **Headers**: `Content-Disposition: attachment; filename="attendance_report_20261028.csv"`
- **Security**: Formula injection characters (`=`, `+`, `-`, `@`) prepended with `'`.

---

## 5. Standard RFC 9457 Problem Details Error Format
All errors return the standard RFC 9457 schema:
```json
{
  "type": "https://smartattendance.com/errors/duplicate-attendance",
  "title": "Conflict",
  "status": 409,
  "detail": "Employee EMP-001 has already clocked in for date 2026-10-28.",
  "instance": "/api/v1/attendance/clock-in",
  "timestamp": "2026-10-28T09:05:00Z",
  "correlationId": "c4b3708a-dfad-42e5-a0cf-61e8c148ce5e",
  "errors": null
}
```

