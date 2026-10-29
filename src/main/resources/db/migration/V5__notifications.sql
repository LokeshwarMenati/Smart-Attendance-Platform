CREATE TABLE notifications (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    recipient_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    link VARCHAR(255),
    type VARCHAR(40) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notifications_public_id UNIQUE (public_id)
);

CREATE INDEX idx_notifications_recipient_created ON notifications(recipient_id, created_at DESC);
CREATE INDEX idx_notifications_recipient_unread ON notifications(recipient_id, read_at);
