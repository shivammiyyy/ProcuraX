INSERT INTO permissions (name, description) VALUES
    ('PAYMENT_MANDATE_MANAGE', 'Create and revoke organization payment mandates')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('PLATFORM_ADMIN', 'ORG_ADMIN', 'FINANCE')
  AND permission.name = 'PAYMENT_MANDATE_MANAGE'
ON CONFLICT DO NOTHING;

CREATE TABLE payment_mandates (
    id                  UUID PRIMARY KEY,
    organization_id     UUID NOT NULL REFERENCES organizations (id),
    purchase_order_id   UUID NOT NULL,
    maximum_amount      NUMERIC(14, 2) NOT NULL CHECK (maximum_amount > 0),
    currency            VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    expires_at          TIMESTAMPTZ NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'REVOKED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_mandate_org_id UNIQUE (organization_id, id),
    CONSTRAINT uq_payment_mandate_org_order_id UNIQUE (organization_id, purchase_order_id, id),
    CONSTRAINT fk_payment_mandate_purchase_order
        FOREIGN KEY (organization_id, purchase_order_id)
        REFERENCES purchase_orders (organization_id, id)
);

CREATE UNIQUE INDEX uq_active_payment_mandate_per_order
    ON payment_mandates (organization_id, purchase_order_id)
    WHERE status = 'ACTIVE';

CREATE TABLE payment_intents (
    id                      UUID PRIMARY KEY,
    organization_id         UUID NOT NULL REFERENCES organizations (id),
    purchase_order_id       UUID NOT NULL,
    mandate_id              UUID NOT NULL,
    amount                  NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    currency                VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    status                  VARCHAR(20) NOT NULL
                            CHECK (status IN ('AUTHORIZED', 'CAPTURED', 'REFUNDED')),
    authorize_idempotency_key VARCHAR(100) NOT NULL,
    capture_idempotency_key VARCHAR(100),
    refund_idempotency_key  VARCHAR(100),
    sandbox_authorization_reference VARCHAR(100) NOT NULL,
    sandbox_capture_reference VARCHAR(100),
    sandbox_refund_reference VARCHAR(100),
    authorized_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    captured_at             TIMESTAMPTZ,
    refunded_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by              UUID,
    updated_by              UUID,
    version                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_intent_org_id UNIQUE (organization_id, id),
    CONSTRAINT uq_payment_intent_org_order UNIQUE (organization_id, purchase_order_id),
    CONSTRAINT uq_payment_authorize_idempotency UNIQUE (organization_id, authorize_idempotency_key),
    CONSTRAINT uq_payment_capture_idempotency UNIQUE (organization_id, capture_idempotency_key),
    CONSTRAINT uq_payment_refund_idempotency UNIQUE (organization_id, refund_idempotency_key),
    CONSTRAINT fk_payment_intent_purchase_order
        FOREIGN KEY (organization_id, purchase_order_id)
        REFERENCES purchase_orders (organization_id, id),
    CONSTRAINT fk_payment_intent_mandate
        FOREIGN KEY (organization_id, purchase_order_id, mandate_id)
        REFERENCES payment_mandates (organization_id, purchase_order_id, id),
    CONSTRAINT ck_payment_capture_state CHECK (
        (status = 'AUTHORIZED' AND captured_at IS NULL AND refunded_at IS NULL
            AND sandbox_capture_reference IS NULL AND sandbox_refund_reference IS NULL
            AND capture_idempotency_key IS NULL AND refund_idempotency_key IS NULL)
        OR (status = 'CAPTURED' AND captured_at IS NOT NULL AND refunded_at IS NULL
            AND sandbox_capture_reference IS NOT NULL AND sandbox_refund_reference IS NULL
            AND capture_idempotency_key IS NOT NULL AND refund_idempotency_key IS NULL)
        OR (status = 'REFUNDED' AND captured_at IS NOT NULL AND refunded_at IS NOT NULL
            AND sandbox_capture_reference IS NOT NULL AND sandbox_refund_reference IS NOT NULL
            AND capture_idempotency_key IS NOT NULL AND refund_idempotency_key IS NOT NULL)
    )
);

CREATE TABLE payment_receipts (
    id                UUID PRIMARY KEY,
    organization_id   UUID NOT NULL REFERENCES organizations (id),
    payment_intent_id UUID NOT NULL,
    operation         VARCHAR(20) NOT NULL CHECK (operation IN ('AUTHORIZED', 'CAPTURED', 'REFUNDED')),
    sandbox_reference VARCHAR(100) NOT NULL,
    amount            NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    currency          VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_receipt_intent
        FOREIGN KEY (organization_id, payment_intent_id)
        REFERENCES payment_intents (organization_id, id),
    CONSTRAINT uq_payment_receipt_operation UNIQUE (organization_id, payment_intent_id, operation),
    CONSTRAINT uq_payment_receipt_sandbox_reference UNIQUE (sandbox_reference)
);

CREATE INDEX idx_payment_intents_org_created
    ON payment_intents (organization_id, created_at DESC);

CREATE INDEX idx_payment_receipts_org_created
    ON payment_receipts (organization_id, created_at DESC);
