package com.procurax.quotation.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vendor_scores")
public class VendorScore extends BaseEntity {

    @Column(name = "quotation_id", nullable = false, updatable = false)
    private UUID quotationId;

    @Column(name = "vendor_id", nullable = false, updatable = false)
    private UUID vendorId;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode factors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode weights;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "correlation_id")
    private UUID correlationId;

    protected VendorScore() {
        super();
    }

    public VendorScore(UUID organizationId, UUID quotationId, UUID vendorId, BigDecimal totalScore,
                       JsonNode factors, JsonNode weights, UUID correlationId) {
        super(organizationId);
        this.quotationId = quotationId;
        this.vendorId = vendorId;
        this.totalScore = totalScore;
        this.factors = factors;
        this.weights = weights;
        this.correlationId = correlationId;
        this.modelVersion = "deterministic-v1";
        this.explanation = "Deterministic score; factor details are shown separately.";
        this.confidence = BigDecimal.ONE;
    }

    public UUID getQuotationId() { return quotationId; }
    public UUID getVendorId() { return vendorId; }
    public BigDecimal getTotalScore() { return totalScore; }
    public JsonNode getFactors() { return factors; }
    public JsonNode getWeights() { return weights; }
    public String getExplanation() { return explanation; }
    public BigDecimal getConfidence() { return confidence; }
    public String getModelVersion() { return modelVersion; }
}
