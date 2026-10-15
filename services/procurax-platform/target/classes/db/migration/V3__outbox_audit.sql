-- Event backbone support: transactional outbox, consumer idempotency, audit trail.

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    organization_id UUID,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(80)  NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    correlation_id  UUID         NOT NULL,
    causation_id    UUID,
    payload         JSONB        NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events (created_at) WHERE status = 'PENDING';

-- One row per (consumer, event); the primary key makes replays no-ops.
CREATE TABLE processed_events (
    consumer     VARCHAR(100) NOT NULL,
    event_id     UUID         NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);

CREATE TABLE audit_events (
    id              UUID PRIMARY KEY,
    organization_id UUID,
    actor_type      VARCHAR(20)  NOT NULL,
    actor_id        VARCHAR(100),
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(50),
    resource_id     VARCHAR(100),
    correlation_id  UUID,
    event_id        UUID,
    details         JSONB        NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_org_time ON audit_events (organization_id, created_at DESC);
CREATE INDEX idx_audit_correlation ON audit_events (correlation_id);
