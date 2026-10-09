CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE contract_ai_review_chunks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    contract_id UUID NOT NULL,
    review_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    start_character INTEGER NOT NULL,
    end_character INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    embedding vector(1024) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_contract_ai_chunk_contract
        FOREIGN KEY (organization_id, contract_id) REFERENCES contracts (organization_id, id),
    CONSTRAINT fk_contract_ai_chunk_review
        FOREIGN KEY (organization_id, review_id) REFERENCES contract_ai_reviews (organization_id, id),
    CONSTRAINT uq_contract_ai_review_chunk UNIQUE (organization_id, review_id, chunk_index),
    CONSTRAINT ck_contract_ai_chunk_offsets CHECK (start_character >= 0 AND end_character > start_character)
);

CREATE INDEX ix_contract_ai_chunks_review
    ON contract_ai_review_chunks (organization_id, contract_id, review_id, chunk_index);

CREATE INDEX ix_contract_ai_chunks_embedding
    ON contract_ai_review_chunks USING hnsw (embedding vector_cosine_ops);
