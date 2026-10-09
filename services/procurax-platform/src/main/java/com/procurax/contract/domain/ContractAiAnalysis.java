package com.procurax.contract.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "contract_ai_analyses")
public class ContractAiAnalysis extends BaseEntity {

    @Column(name = "contract_id", nullable = false, updatable = false)
    private UUID contractId;

    @Column(name = "review_id", nullable = false, updatable = false)
    private UUID reviewId;

    @Column(name = "model", nullable = false, length = 100, updatable = false)
    private String model;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_result", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode analysisResult;

    protected ContractAiAnalysis() {
        super();
    }

    public ContractAiAnalysis(UUID organizationId, UUID contractId, UUID reviewId, String model,
                              JsonNode analysisResult) {
        super(organizationId);
        this.contractId = contractId;
        this.reviewId = reviewId;
        this.model = model;
        this.analysisResult = analysisResult.deepCopy();
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getReviewId() {
        return reviewId;
    }

    public String getModel() {
        return model;
    }

    public JsonNode getAnalysisResult() {
        return analysisResult;
    }
}
