package com.procurax.payment.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_mandates")
public class PaymentMandate extends BaseEntity {

    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;

    @Column(name = "maximum_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal maximumAmount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    protected PaymentMandate() {
        super();
    }

    public PaymentMandate(UUID organizationId, UUID purchaseOrderId, BigDecimal maximumAmount,
                          String currency, Instant expiresAt) {
        super(organizationId);
        this.purchaseOrderId = purchaseOrderId;
        this.maximumAmount = maximumAmount;
        this.currency = currency;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException("Only an active payment mandate can be revoked");
        }
        status = "REVOKED";
    }

    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public BigDecimal getMaximumAmount() { return maximumAmount; }
    public String getCurrency() { return currency; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getStatus() { return status; }
}
