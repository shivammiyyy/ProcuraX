CREATE TABLE contract_ai_reviews (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    contract_id UUID NOT NULL,
    review_result JSONB NOT NULL,
    retrieval_method VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_contract_ai_review_contract
        FOREIGN KEY (organization_id, contract_id) REFERENCES contracts (organization_id, id),
    CONSTRAINT ck_contract_ai_review_method CHECK (retrieval_method = 'ephemeral_bm25')
);

CREATE INDEX ix_contract_ai_reviews_history
    ON contract_ai_reviews (organization_id, contract_id, created_at DESC);
