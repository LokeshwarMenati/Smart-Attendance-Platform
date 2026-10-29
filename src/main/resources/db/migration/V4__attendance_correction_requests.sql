CREATE TABLE attendance_correction_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    attendance_id BIGINT NOT NULL REFERENCES attendances(id) ON DELETE RESTRICT,
    requester_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    proposed_clock_in TIMESTAMPTZ NOT NULL,
    proposed_clock_out TIMESTAMPTZ,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT REFERENCES employees(id) ON DELETE RESTRICT,
    decision_reason VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_correction_request_public_id UNIQUE (public_id),
    CONSTRAINT chk_correction_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT chk_correction_request_times CHECK (proposed_clock_out IS NULL OR proposed_clock_out >= proposed_clock_in)
);

CREATE INDEX idx_correction_requests_status_created ON attendance_correction_requests(status, created_at DESC);
CREATE INDEX idx_correction_requests_requester ON attendance_correction_requests(requester_id);
