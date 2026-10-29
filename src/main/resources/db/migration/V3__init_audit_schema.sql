-- V3__init_audit_schema.sql
-- Create audit_logs table for enterprise action tracking

CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id UUID,
    actor_email VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    target_resource VARCHAR(50) NOT NULL,
    target_id VARCHAR(100),
    ip_address VARCHAR(45),
    correlation_id VARCHAR(64),
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_target_resource ON audit_logs(target_resource);

