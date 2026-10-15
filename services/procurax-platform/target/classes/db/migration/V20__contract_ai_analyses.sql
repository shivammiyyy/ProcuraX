ALTER TABLE contract_ai_reviews
    ADD CONSTRAINT uq_contract_ai_review_tenant UNIQUE (organization_id, id);

CREATE TABLE contract_ai_analyses (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    contract_id UUID NOT NULL,
    review_id UUID NOT NULL,
    model VARCHAR(100) NOT NULL,
    analysis_result JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_contract_ai_analysis_contract
        FOREIGN KEY (organization_id, contract_id) REFERENCES contracts (organization_id, id),
    CONSTRAINT fk_contract_ai_analysis_review
        FOREIGN KEY (organization_id, review_id) REFERENCES contract_ai_reviews (organization_id, id)
);

CREATE INDEX ix_contract_ai_analyses_history
    ON contract_ai_analyses (organization_id, contract_id, created_at DESC);
