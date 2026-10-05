package com.procurax.payment.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_receipts")
public class PaymentReceipt extends BaseEntity {

    @Column(name = "payment_intent_id", nullable = false, updatable = false)
    private UUID paymentIntentId;

    @Column(nullable = false, length = 20, updatable = false)
    private String operation;

    @Column(name = "sandbox_reference", nullable = false, length = 100, updatable = false)
    private String sandboxReference;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    protected PaymentReceipt() {
        super();
    }

    public PaymentReceipt(UUID organizationId, UUID paymentIntentId, String operation,
                          String sandboxReference, BigDecimal amount, String currency) {
        super(organizationId);
        this.paymentIntentId = paymentIntentId;
        this.operation = operation;
        this.sandboxReference = sandboxReference;
        this.amount = amount;
        this.currency = currency;
    }

    public UUID getPaymentIntentId() { return paymentIntentId; }
    public String getOperation() { return operation; }
    public String getSandboxReference() { return sandboxReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
