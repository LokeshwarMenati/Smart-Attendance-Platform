-- V1__init_employee_schema.sql
-- Create employees table with internal BIGINT identity and external UUID public_id

CREATE TABLE employees (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    employee_code VARCHAR(30) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employees_public_id UNIQUE (public_id),
    CONSTRAINT uq_employees_code UNIQUE (employee_code),
    CONSTRAINT uq_employees_email UNIQUE (email),
    CONSTRAINT chk_employees_role CHECK (role IN ('ADMIN', 'MANAGER', 'EMPLOYEE')),
    CONSTRAINT chk_employees_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_employees_email ON employees(email);
CREATE INDEX idx_employees_code ON employees(employee_code);
CREATE INDEX idx_employees_public_id ON employees(public_id);

