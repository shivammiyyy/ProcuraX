ALTER TABLE outbox_events
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN last_attempt_at TIMESTAMPTZ,
    ADD COLUMN last_error VARCHAR(2000),
    ADD COLUMN dead_lettered_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_outbox_delivery_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'DEAD_LETTERED')),
    ADD CONSTRAINT ck_outbox_retry_count
        CHECK (retry_count >= 0),
    ADD CONSTRAINT ck_outbox_delivery_metadata
        CHECK (
            (status = 'PENDING' AND published_at IS NULL AND dead_lettered_at IS NULL)
            OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND dead_lettered_at IS NULL)
            OR (status = 'DEAD_LETTERED' AND published_at IS NULL AND dead_lettered_at IS NOT NULL)
        );

DROP INDEX idx_outbox_pending;
CREATE INDEX idx_outbox_pending
    ON outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';
