package com.procurax.fulfillment.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment extends BaseEntity {

    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;

    @Column(name = "tracking_number", nullable = false, length = 100, updatable = false)
    private String trackingNumber;

    @Column(nullable = false, length = 120, updatable = false)
    private String carrier;

    @Column(nullable = false, length = 20)
    private String status = "IN_TRANSIT";

    @Column(name = "expected_at")
    private Instant expectedAt;

    @Column(name = "shipped_at", nullable = false, updatable = false)
    private Instant shippedAt = Instant.now();

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "exception_reason", length = 1000)
    private String exceptionReason;

    protected Shipment() {
        super();
    }

    public Shipment(UUID organizationId, UUID purchaseOrderId, String trackingNumber,
                    String carrier, Instant expectedAt) {
        super(organizationId);
        this.purchaseOrderId = purchaseOrderId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.expectedAt = expectedAt;
    }

    public void deliver() {
        if (!"IN_TRANSIT".equals(status)) {
            throw new IllegalStateException("Only an in-transit shipment can be delivered");
        }
        status = "DELIVERED";
        deliveredAt = Instant.now();
    }

    public void reportException(String reason) {
        if (!"IN_TRANSIT".equals(status)) {
            throw new IllegalStateException("Only an in-transit shipment can be updated");
        }
        status = "EXCEPTION";
        exceptionReason = reason;
    }

    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getCarrier() { return carrier; }
    public String getStatus() { return status; }
    public Instant getExpectedAt() { return expectedAt; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public String getExceptionReason() { return exceptionReason; }
}
