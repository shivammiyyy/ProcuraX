ALTER TABLE approval_requests
    ADD COLUMN rfq_id UUID,
    ADD COLUMN quotation_id UUID,
    ADD CONSTRAINT ck_approval_request_procurement_link CHECK (
        (rfq_id IS NULL AND quotation_id IS NULL)
        OR (rfq_id IS NOT NULL AND quotation_id IS NOT NULL)
    ),
    ADD CONSTRAINT uq_approval_request_procurement_ref
        UNIQUE (organization_id, rfq_id, quotation_id, id);

ALTER TABLE quotations
    ADD CONSTRAINT uq_quotation_org_rfq_id UNIQUE (organization_id, rfq_id, id);

ALTER TABLE approval_requests
    ADD CONSTRAINT fk_approval_request_quotation_rfq
        FOREIGN KEY (organization_id, rfq_id, quotation_id)
        REFERENCES quotations (organization_id, rfq_id, id);

CREATE TABLE purchase_orders (
    id                  UUID PRIMARY KEY,
    organization_id     UUID NOT NULL REFERENCES organizations (id),
    po_number           VARCHAR(40) NOT NULL,
    approval_request_id UUID NOT NULL,
    rfq_id              UUID NOT NULL,
    quotation_id        UUID NOT NULL,
    vendor_id           UUID NOT NULL,
    vendor_name         VARCHAR(200) NOT NULL,
    vendor_contact_email VARCHAR(320),
    total_amount        NUMERIC(14, 2) NOT NULL CHECK (total_amount > 0),
    currency            VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    delivery_days       INT NOT NULL CHECK (delivery_days > 0),
    payment_terms_days  INT NOT NULL CHECK (payment_terms_days >= 0),
    status              VARCHAR(20) NOT NULL DEFAULT 'ISSUED'
                        CHECK (status IN ('ISSUED', 'CANCELLED')),
    issued_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_purchase_order_org_id UNIQUE (organization_id, id),
    CONSTRAINT uq_purchase_order_org_number UNIQUE (organization_id, po_number),
    CONSTRAINT uq_purchase_order_org_approval UNIQUE (organization_id, approval_request_id),
    CONSTRAINT uq_purchase_order_org_quotation UNIQUE (organization_id, quotation_id),
    CONSTRAINT fk_purchase_order_approval
        FOREIGN KEY (organization_id, rfq_id, quotation_id, approval_request_id)
        REFERENCES approval_requests (organization_id, rfq_id, quotation_id, id),
    CONSTRAINT fk_purchase_order_rfq
        FOREIGN KEY (organization_id, rfq_id) REFERENCES rfqs (organization_id, id),
    CONSTRAINT fk_purchase_order_quotation_rfq
        FOREIGN KEY (organization_id, rfq_id, quotation_id)
        REFERENCES quotations (organization_id, rfq_id, id),
    CONSTRAINT fk_purchase_order_vendor
        FOREIGN KEY (organization_id, vendor_id) REFERENCES vendors (organization_id, id)
);

CREATE INDEX idx_purchase_orders_org_created
    ON purchase_orders (organization_id, created_at DESC);

CREATE TABLE purchase_order_items (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations (id),
    purchase_order_id UUID NOT NULL,
    rfq_item_id     UUID NOT NULL,
    description     VARCHAR(500) NOT NULL,
    specification   TEXT,
    quantity        INT NOT NULL CHECK (quantity > 0),
    unit            VARCHAR(30),
    unit_price      NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    line_total      NUMERIC(14, 2) NOT NULL CHECK (line_total >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_purchase_order_item_parent
        FOREIGN KEY (organization_id, purchase_order_id)
        REFERENCES purchase_orders (organization_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_order_item_rfq_item
        FOREIGN KEY (organization_id, rfq_item_id) REFERENCES rfq_items (organization_id, id),
    CONSTRAINT ck_purchase_order_item_total
        CHECK (line_total = unit_price * quantity)
);

CREATE INDEX idx_purchase_order_items_parent
    ON purchase_order_items (organization_id, purchase_order_id);
