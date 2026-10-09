package com.procurax.approval.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "approval_requests")
public class ApprovalRequest extends BaseEntity {

    @Column(name = "requester_user_id", nullable = false, updatable = false)
    private UUID requesterUserId;

    @Column(name = "rfq_id", updatable = false)
    private UUID rfqId;

    @Column(name = "quotation_id", updatable = false)
    private UUID quotationId;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "quotation_count", nullable = false)
    private int quotationCount;

    @Column(nullable = false, length = 2000)
    private String justification;

    @Column(name = "policy_evaluation_id", nullable = false, updatable = false)
    private UUID policyEvaluationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy_decisions", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode policyDecisions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    protected ApprovalRequest() {
        super();
    }

    public ApprovalRequest(UUID organizationId, UUID requesterUserId, UUID rfqId, UUID quotationId,
                           String subject, String category,
                           BigDecimal amount, String currency, int quotationCount, String justification,
                           UUID policyEvaluationId, JsonNode policyDecisions) {
        super(organizationId);
        this.requesterUserId = requesterUserId;
        this.rfqId = rfqId;
        this.quotationId = quotationId;
        this.subject = subject;
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.quotationCount = quotationCount;
        this.justification = justification;
        this.policyEvaluationId = policyEvaluationId;
        this.policyDecisions = policyDecisions;
    }

    public void decide(ApprovalStatus decision) {
        if (decision == ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("An approval decision must be final");
        }
        this.status = decision;
    }

    public UUID getRequesterUserId() { return requesterUserId; }
    public UUID getRfqId() { return rfqId; }
    public UUID getQuotationId() { return quotationId; }
    public String getSubject() { return subject; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public int getQuotationCount() { return quotationCount; }
    public String getJustification() { return justification; }
    public UUID getPolicyEvaluationId() { return policyEvaluationId; }
    public JsonNode getPolicyDecisions() { return policyDecisions; }
    public ApprovalStatus getStatus() { return status; }
}
