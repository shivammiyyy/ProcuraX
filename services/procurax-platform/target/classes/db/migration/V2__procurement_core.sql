-- Procurement core: vendors, intents, RFQs, quotations, scoring, contracts.
-- Every table carries organization_id; all reads/writes are tenant-scoped in the application layer.

CREATE TABLE vendors (
    id                 UUID PRIMARY KEY,
    organization_id    UUID         NOT NULL REFERENCES organizations (id),
    name               VARCHAR(200) NOT NULL,
    contact_email      VARCHAR(320),
    category           VARCHAR(100),
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    risk_level         VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    payment_terms_days INT          NOT NULL DEFAULT 30,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by         UUID,
    updated_by         UUID,
    version            BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_vendor_name UNIQUE (organization_id, name)
);

CREATE TABLE vendor_documents (
    id                  UUID PRIMARY KEY,
    organization_id     UUID         NOT NULL REFERENCES organizations (id),
    vendor_id           UUID         NOT NULL REFERENCES vendors (id),
    doc_type            VARCHAR(50)  NOT NULL,
    file_name           VARCHAR(255) NOT NULL,
    storage_url         VARCHAR(1000) NOT NULL,
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id              UUID PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(100) NOT NULL,
    sku             VARCHAR(100),
    unit_price      NUMERIC(14, 2),
    currency        CHAR(3)      NOT NULL DEFAULT 'INR',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE procurement_intents (
    id                     UUID PRIMARY KEY,
    organization_id        UUID          NOT NULL REFERENCES organizations (id),
    user_id                UUID          NOT NULL REFERENCES users (id),
    request_text           TEXT          NOT NULL,
    category               VARCHAR(100),
    quantity               INT,
    budget                 NUMERIC(14, 2),
    currency               CHAR(3)       NOT NULL DEFAULT 'INR',
    delivery_deadline_days INT,
    constraints            JSONB         NOT NULL DEFAULT '[]',
    status                 VARCHAR(30)   NOT NULL DEFAULT 'CREATED',
    correlation_id         UUID          NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by             UUID,
    updated_by             UUID,
    version                BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_intents_org ON procurement_intents (organization_id, status);
CREATE INDEX idx_intents_correlation ON procurement_intents (correlation_id);

CREATE TABLE rfqs (
    id              UUID PRIMARY KEY,
    organization_id UUID          NOT NULL REFERENCES organizations (id),
    intent_id       UUID REFERENCES procurement_intents (id),
    title           VARCHAR(255)  NOT NULL,
    description     TEXT          NOT NULL,
    request_type    VARCHAR(5)    NOT NULL DEFAULT 'RFQ',
    category        VARCHAR(100)  NOT NULL,
    budget          NUMERIC(14, 2) NOT NULL,
    currency        CHAR(3)       NOT NULL DEFAULT 'INR',
    deadline        TIMESTAMPTZ   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    correlation_id  UUID,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_rfq_budget CHECK (budget > 0)
);
CREATE INDEX idx_rfqs_org ON rfqs (organization_id, status);

CREATE TABLE rfq_items (
    id              UUID PRIMARY KEY,
    organization_id UUID          NOT NULL REFERENCES organizations (id),
    rfq_id          UUID          NOT NULL REFERENCES rfqs (id) ON DELETE CASCADE,
    description     VARCHAR(500)  NOT NULL,
    quantity        INT           NOT NULL CHECK (quantity > 0),
    unit            VARCHAR(30),
    specification   TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE quotations (
    id                 UUID PRIMARY KEY,
    organization_id    UUID           NOT NULL REFERENCES organizations (id),
    rfq_id             UUID           NOT NULL REFERENCES rfqs (id),
    vendor_id          UUID           NOT NULL REFERENCES vendors (id),
    total_amount       NUMERIC(14, 2) NOT NULL CHECK (total_amount > 0),
    currency           CHAR(3)        NOT NULL DEFAULT 'INR',
    delivery_days      INT            NOT NULL CHECK (delivery_days > 0),
    payment_terms_days INT            NOT NULL DEFAULT 30,
    quality_rating     NUMERIC(4, 2),
    compliance         JSONB          NOT NULL DEFAULT '{}',
    status             VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    correlation_id     UUID,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by         UUID,
    updated_by         UUID,
    version            BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_quote_vendor_rfq UNIQUE (rfq_id, vendor_id)
);
CREATE INDEX idx_quotes_org ON quotations (organization_id, rfq_id);

CREATE TABLE quotation_items (
    id              UUID PRIMARY KEY,
    organization_id UUID           NOT NULL REFERENCES organizations (id),
    quotation_id    UUID           NOT NULL REFERENCES quotations (id) ON DELETE CASCADE,
    rfq_item_id     UUID REFERENCES rfq_items (id),
    unit_price      NUMERIC(14, 2) NOT NULL CHECK (unit_price >= 0),
    quantity        INT            NOT NULL CHECK (quantity > 0),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT         NOT NULL DEFAULT 0
);

-- The official score is computed deterministically by Spring Boot from the weights stored here;
-- AI/ML output is kept as explanation and confidence only.
CREATE TABLE vendor_scores (
    id                UUID PRIMARY KEY,
    organization_id   UUID          NOT NULL REFERENCES organizations (id),
    quotation_id      UUID          NOT NULL REFERENCES quotations (id),
    vendor_id         UUID          NOT NULL REFERENCES vendors (id),
    total_score       NUMERIC(5, 2) NOT NULL,
    factors           JSONB         NOT NULL,
    weights           JSONB         NOT NULL,
    explanation       TEXT,
    confidence        NUMERIC(4, 3),
    model_version     VARCHAR(50),
    correlation_id    UUID,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_scores_quote ON vendor_scores (quotation_id);

CREATE TABLE contracts (
    id              UUID PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    rfq_id          UUID         NOT NULL REFERENCES rfqs (id),
    quotation_id    UUID         NOT NULL REFERENCES quotations (id),
    vendor_id       UUID         NOT NULL REFERENCES vendors (id),
    content         TEXT         NOT NULL,
    file_url        VARCHAR(1000),
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    audit_status    VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_contract_dates CHECK (end_date >= start_date)
);

CREATE TABLE contract_audits (
    id              UUID PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    contract_id     UUID         NOT NULL REFERENCES contracts (id) ON DELETE CASCADE,
    risk_level      VARCHAR(10)  NOT NULL,
    finding         TEXT         NOT NULL,
    clause          TEXT,
    explanation     TEXT,
    recommendation  TEXT,
    confidence      NUMERIC(4, 3),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT       NOT NULL DEFAULT 0
);
