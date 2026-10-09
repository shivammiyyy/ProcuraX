CREATE TABLE contract_decisions (
    id              UUID PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    contract_id     UUID         NOT NULL,
    decision        VARCHAR(20)  NOT NULL,
    comment         TEXT         NOT NULL,
    decided_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_contract_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT uq_contract_decision_org_contract UNIQUE (organization_id, contract_id),
    CONSTRAINT fk_contract_decision_parent
        FOREIGN KEY (organization_id, contract_id) REFERENCES contracts (organization_id, id)
);

CREATE INDEX idx_contract_decisions_contract ON contract_decisions (organization_id, contract_id);
