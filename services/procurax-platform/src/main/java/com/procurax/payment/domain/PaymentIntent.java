package com.procurax.payment.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents")
public class PaymentIntent extends BaseEntity {

    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;

    @Column(name = "mandate_id", nullable = false, updatable = false)
    private UUID mandateId;

    @Column(nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status = "AUTHORIZED";

    @Column(name = "authorize_idempotency_key", nullable = false, length = 100, updatable = false)
    private String authorizeIdempotencyKey;

    @Column(name = "capture_idempotency_key", length = 100)
    private String captureIdempotencyKey;

    @Column(name = "refund_idempotency_key", length = 100)
    private String refundIdempotencyKey;

    @Column(name = "sandbox_authorization_reference", nullable = false, length = 100, updatable = false)
    private String sandboxAuthorizationReference;

    @Column(name = "sandbox_capture_reference", length = 100)
    private String sandboxCaptureReference;

    @Column(name = "sandbox_refund_reference", length = 100)
    private String sandboxRefundReference;

    @Column(name = "authorized_at", nullable = false, updatable = false)
    private Instant authorizedAt = Instant.now();

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    protected PaymentIntent() {
        super();
    }

    public PaymentIntent(UUID organizationId, UUID purchaseOrderId, UUID mandateId, BigDecimal amount,
                         String currency, String authorizeIdempotencyKey,
                         String sandboxAuthorizationReference) {
        super(organizationId);
        this.purchaseOrderId = purchaseOrderId;
        this.mandateId = mandateId;
        this.amount = amount;
        this.currency = currency;
        this.authorizeIdempotencyKey = authorizeIdempotencyKey;
        this.sandboxAuthorizationReference = sandboxAuthorizationReference;
    }

    public void capture(String idempotencyKey, String sandboxReference) {
        if (!"AUTHORIZED".equals(status)) {
            throw new IllegalStateException("Only an authorized payment can be captured");
        }
        status = "CAPTURED";
        captureIdempotencyKey = idempotencyKey;
        sandboxCaptureReference = sandboxReference;
        capturedAt = Instant.now();
    }

    public void refund(String idempotencyKey, String sandboxReference) {
        if (!"CAPTURED".equals(status)) {
            throw new IllegalStateException("Only a captured payment can be refunded");
        }
        status = "REFUNDED";
        refundIdempotencyKey = idempotencyKey;
        sandboxRefundReference = sandboxReference;
        refundedAt = Instant.now();
    }

    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public UUID getMandateId() { return mandateId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getAuthorizeIdempotencyKey() { return authorizeIdempotencyKey; }
    public String getCaptureIdempotencyKey() { return captureIdempotencyKey; }
    public String getRefundIdempotencyKey() { return refundIdempotencyKey; }
    public String getSandboxAuthorizationReference() { return sandboxAuthorizationReference; }
    public String getSandboxCaptureReference() { return sandboxCaptureReference; }
    public String getSandboxRefundReference() { return sandboxRefundReference; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public Instant getCapturedAt() { return capturedAt; }
    public Instant getRefundedAt() { return refundedAt; }
}
