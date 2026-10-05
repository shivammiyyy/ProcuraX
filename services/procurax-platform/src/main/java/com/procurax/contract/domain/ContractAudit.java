package com.procurax.contract.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "contract_audits")
public class ContractAudit extends BaseEntity {

    @Column(name = "contract_id", nullable = false, updatable = false)
    private UUID contractId;

    @Column(name = "risk_level", nullable = false, length = 10)
    private String riskLevel;

    @Column(nullable = false, columnDefinition = "text")
    private String finding;

    @Column(columnDefinition = "text")
    private String clause;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(columnDefinition = "text")
    private String recommendation;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    protected ContractAudit() {
        super();
    }

    public ContractAudit(UUID organizationId, UUID contractId, String riskLevel, String finding,
                         String clause, String explanation, String recommendation, BigDecimal confidence) {
        super(organizationId);
        this.contractId = contractId;
        this.riskLevel = riskLevel;
        this.finding = finding;
        this.clause = clause;
        this.explanation = explanation;
        this.recommendation = recommendation;
        this.confidence = confidence;
    }

    public UUID getContractId() { return contractId; }
    public String getRiskLevel() { return riskLevel; }
    public String getFinding() { return finding; }
    public String getClause() { return clause; }
    public String getExplanation() { return explanation; }
    public String getRecommendation() { return recommendation; }
    public BigDecimal getConfidence() { return confidence; }
}
