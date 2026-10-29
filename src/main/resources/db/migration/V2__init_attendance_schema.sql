-- V2__init_attendance_schema.sql
-- Create attendances table with strict business date uniqueness constraint

CREATE TABLE attendances (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    attendance_date DATE NOT NULL,
    clock_in_time TIMESTAMPTZ NOT NULL,
    clock_out_time TIMESTAMPTZ,
    duration_minutes INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendances_public_id UNIQUE (public_id),
    -- Business Invariant: Only one attendance record per employee per business date
    CONSTRAINT uq_attendances_employee_date UNIQUE (employee_id, attendance_date),
    CONSTRAINT chk_attendances_status CHECK (status IN ('OPEN', 'COMPLETED')),
    CONSTRAINT chk_clock_out_after_in CHECK (clock_out_time IS NULL OR clock_out_time >= clock_in_time)
);

CREATE INDEX idx_attendances_date_status ON attendances(attendance_date, status);
CREATE INDEX idx_attendances_employee_id ON attendances(employee_id);
CREATE INDEX idx_attendances_public_id ON attendances(public_id);

