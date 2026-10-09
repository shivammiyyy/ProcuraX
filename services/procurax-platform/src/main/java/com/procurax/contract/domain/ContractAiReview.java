package com.procurax.contract.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "contract_ai_reviews")
public class ContractAiReview extends BaseEntity {

    @Column(name = "contract_id", nullable = false, updatable = false)
    private UUID contractId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "review_result", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode reviewResult;

    @Column(name = "retrieval_method", nullable = false, length = 40, updatable = false)
    private String retrievalMethod;

    protected ContractAiReview() {
        super();
    }

    public ContractAiReview(UUID organizationId, UUID contractId, JsonNode reviewResult,
                           String retrievalMethod) {
        super(organizationId);
        this.contractId = contractId;
        this.reviewResult = reviewResult.deepCopy();
        this.retrievalMethod = retrievalMethod;
    }

    public UUID getContractId() {
        return contractId;
    }

    public JsonNode getReviewResult() {
        return reviewResult;
    }

    public String getRetrievalMethod() {
        return retrievalMethod;
    }
}
