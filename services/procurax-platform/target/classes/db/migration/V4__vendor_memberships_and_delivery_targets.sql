ALTER TABLE rfqs
    ADD COLUMN delivery_days INT NOT NULL DEFAULT 14 CHECK (delivery_days > 0);

ALTER TABLE vendors
    ADD COLUMN performance_score NUMERIC(5, 2) NOT NULL DEFAULT 50
        CHECK (performance_score >= 0 AND performance_score <= 100);

CREATE TABLE vendor_memberships (
    id              UUID PRIMARY KEY,
    organization_id UUID        NOT NULL REFERENCES organizations (id),
    vendor_id       UUID        NOT NULL REFERENCES vendors (id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    CONSTRAINT uq_vendor_membership UNIQUE (organization_id, vendor_id, user_id)
);
CREATE INDEX idx_vendor_memberships_user
    ON vendor_memberships (organization_id, user_id);

ALTER TABLE vendor_scores
    ADD CONSTRAINT uq_vendor_scores_quotation UNIQUE (quotation_id);
