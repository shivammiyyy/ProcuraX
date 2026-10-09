package com.procurax.fulfillment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun extends BaseEntity {

    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;

    @Column(name = "invoice_id", nullable = false, updatable = false)
    private UUID invoiceId;

    @Column(name = "payment_intent_id", updatable = false)
    private UUID paymentIntentId;

    @Column(nullable = false, length = 20, updatable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode findings;

    @Column(name = "reconciled_at", nullable = false, updatable = false)
    private Instant reconciledAt = Instant.now();

    protected ReconciliationRun() {
        super();
    }

    public ReconciliationRun(UUID organizationId, UUID purchaseOrderId, UUID invoiceId,
                            UUID paymentIntentId, String status, JsonNode findings) {
        super(organizationId);
        this.purchaseOrderId = purchaseOrderId;
        this.invoiceId = invoiceId;
        this.paymentIntentId = paymentIntentId;
        this.status = status;
        this.findings = findings;
    }

    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public UUID getInvoiceId() { return invoiceId; }
    public UUID getPaymentIntentId() { return paymentIntentId; }
    public String getStatus() { return status; }
    public JsonNode getFindings() { return findings; }
    public Instant getReconciledAt() { return reconciledAt; }
}
