INSERT INTO permissions (name, description) VALUES
    ('SHIPMENT_READ', 'Read organization shipment records'),
    ('SHIPMENT_MANAGE', 'Record and update organization shipments'),
    ('INVOICE_READ', 'Read organization invoice records'),
    ('INVOICE_MANAGE', 'Record organization vendor invoices'),
    ('RECONCILIATION_READ', 'Read organization reconciliation results'),
    ('RECONCILIATION_EXECUTE', 'Run organization purchase reconciliations')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('PLATFORM_ADMIN', 'ORG_ADMIN', 'FINANCE')
  AND permission.name IN (
      'SHIPMENT_READ', 'SHIPMENT_MANAGE', 'INVOICE_READ', 'INVOICE_MANAGE',
      'RECONCILIATION_READ', 'RECONCILIATION_EXECUTE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.name IN (
    'SHIPMENT_READ', 'SHIPMENT_MANAGE', 'INVOICE_READ', 'INVOICE_MANAGE',
    'RECONCILIATION_READ', 'RECONCILIATION_EXECUTE')
WHERE role.name = 'PROCUREMENT_MANAGER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.name IN (
    'SHIPMENT_READ', 'INVOICE_READ', 'RECONCILIATION_READ')
WHERE role.name IN ('BUYER', 'APPROVER')
ON CONFLICT DO NOTHING;

ALTER TABLE purchase_order_items
    ADD CONSTRAINT uq_purchase_order_item_org_order_id
    UNIQUE (organization_id, purchase_order_id, id);

CREATE TABLE shipments (
    id                UUID PRIMARY KEY,
    organization_id   UUID NOT NULL REFERENCES organizations (id),
    purchase_order_id UUID NOT NULL,
    tracking_number   VARCHAR(100) NOT NULL,
    carrier           VARCHAR(120) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'IN_TRANSIT'
                      CHECK (status IN ('IN_TRANSIT', 'DELIVERED', 'EXCEPTION')),
    expected_at       TIMESTAMPTZ,
    shipped_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at      TIMESTAMPTZ,
    exception_reason  VARCHAR(1000),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_shipment_org_id UNIQUE (organization_id, id),
    CONSTRAINT uq_shipment_org_order_id UNIQUE (organization_id, purchase_order_id, id),
    CONSTRAINT uq_shipment_tracking UNIQUE (organization_id, tracking_number),
    CONSTRAINT fk_shipment_purchase_order
        FOREIGN KEY (organization_id, purchase_order_id)
        REFERENCES purchase_orders (organization_id, id),
    CONSTRAINT ck_shipment_delivery_state CHECK (
        (status = 'IN_TRANSIT' AND delivered_at IS NULL AND exception_reason IS NULL)
        OR (status = 'DELIVERED' AND delivered_at IS NOT NULL AND exception_reason IS NULL)
        OR (status = 'EXCEPTION' AND exception_reason IS NOT NULL AND length(trim(exception_reason)) > 0)
    )
);

CREATE INDEX idx_shipments_org_po
    ON shipments (organization_id, purchase_order_id, created_at);

CREATE TABLE shipment_items (
    id                    UUID PRIMARY KEY,
    organization_id       UUID NOT NULL REFERENCES organizations (id),
    shipment_id           UUID NOT NULL,
    purchase_order_id     UUID NOT NULL,
    purchase_order_item_id UUID NOT NULL,
    description           VARCHAR(500) NOT NULL,
    quantity              INT NOT NULL CHECK (quantity > 0),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_shipment_item_parent
        FOREIGN KEY (organization_id, purchase_order_id, shipment_id)
        REFERENCES shipments (organization_id, purchase_order_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_item_order
        FOREIGN KEY (organization_id, purchase_order_id)
        REFERENCES purchase_orders (organization_id, id),
    CONSTRAINT fk_shipment_item_po_line
        FOREIGN KEY (organization_id, purchase_order_id, purchase_order_item_id)
        REFERENCES purchase_order_items (organization_id, purchase_order_id, id),
    CONSTRAINT uq_shipment_item_po_line UNIQUE (organization_id, shipment_id, purchase_order_item_id)
);

CREATE TABLE invoices (
    id                UUID PRIMARY KEY,
    organization_id   UUID NOT NULL REFERENCES organizations (id),
    purchase_order_id UUID NOT NULL,
    invoice_number    VARCHAR(100) NOT NULL,
    invoice_date      DATE NOT NULL,
    due_date          DATE,
    amount            NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    currency          VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    status            VARCHAR(20) NOT NULL DEFAULT 'RECORDED'
                      CHECK (status IN ('RECORDED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_invoice_org_id UNIQUE (organization_id, id),
    CONSTRAINT uq_invoice_org_order UNIQUE (organization_id, purchase_order_id),
    CONSTRAINT fk_invoice_purchase_order
        FOREIGN KEY (organization_id, purchase_order_id)
        REFERENCES purchase_orders (organization_id, id),
    CONSTRAINT ck_invoice_due_date CHECK (due_date IS NULL OR due_date >= invoice_date)
);

CREATE UNIQUE INDEX uq_invoice_org_number_ci
    ON invoices (organization_id, lower(invoice_number));

CREATE INDEX idx_invoices_org_created
    ON invoices (organization_id, created_at DESC);

ALTER TABLE invoices
    ADD CONSTRAINT uq_invoice_org_order_id UNIQUE (organization_id, purchase_order_id, id);

ALTER TABLE payment_intents
    ADD CONSTRAINT uq_payment_intent_org_order_id UNIQUE (organization_id, purchase_order_id, id);

CREATE TABLE reconciliation_runs (
    id                UUID PRIMARY KEY,
    organization_id   UUID NOT NULL REFERENCES organizations (id),
    purchase_order_id UUID NOT NULL,
    invoice_id        UUID NOT NULL,
    payment_intent_id UUID,
    status            VARCHAR(20) NOT NULL CHECK (status IN ('MATCHED', 'EXCEPTION')),
    findings          JSONB NOT NULL CHECK (jsonb_typeof(findings) = 'array'),
    reconciled_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reconciliation_po
        FOREIGN KEY (organization_id, purchase_order_id)
        REFERENCES purchase_orders (organization_id, id),
    CONSTRAINT fk_reconciliation_invoice
        FOREIGN KEY (organization_id, purchase_order_id, invoice_id)
        REFERENCES invoices (organization_id, purchase_order_id, id),
    CONSTRAINT fk_reconciliation_payment
        FOREIGN KEY (organization_id, purchase_order_id, payment_intent_id)
        REFERENCES payment_intents (organization_id, purchase_order_id, id)
);

CREATE INDEX idx_reconciliation_runs_org_created
    ON reconciliation_runs (organization_id, reconciled_at DESC);
