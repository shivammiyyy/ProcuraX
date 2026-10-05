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
@Table(name = "quotations")
public class Quotation extends BaseEntity {

    @Column(name = "rfq_id", nullable = false, updatable = false)
    private UUID rfqId;

    @Column(name = "vendor_id", nullable = false, updatable = false)
    private UUID vendorId;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "delivery_days", nullable = false)
    private int deliveryDays;

    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays;

    @Column(name = "quality_rating", precision = 4, scale = 2)
    private BigDecimal qualityRating;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode compliance;

    @Column(nullable = false, length = 20)
    private String status = "SUBMITTED";

    @Column(name = "correlation_id")
    private UUID correlationId;

    protected Quotation() {
        super();
    }

    public Quotation(UUID organizationId, UUID rfqId, UUID vendorId, BigDecimal totalAmount,
                     String currency, int deliveryDays, int paymentTermsDays,
                     BigDecimal qualityRating, JsonNode compliance, UUID correlationId) {
        super(organizationId);
        this.rfqId = rfqId;
        this.vendorId = vendorId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.deliveryDays = deliveryDays;
        this.paymentTermsDays = paymentTermsDays;
        this.qualityRating = qualityRating;
        this.compliance = compliance;
        this.correlationId = correlationId;
    }

    public UUID getRfqId() { return rfqId; }
    public UUID getVendorId() { return vendorId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public int getDeliveryDays() { return deliveryDays; }
    public int getPaymentTermsDays() { return paymentTermsDays; }
    public BigDecimal getQualityRating() { return qualityRating; }
    public JsonNode getCompliance() { return compliance; }
    public String getStatus() { return status; }
    public UUID getCorrelationId() { return correlationId; }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void replace(BigDecimal totalAmount, String currency, int deliveryDays, int paymentTermsDays,
                        BigDecimal qualityRating, JsonNode compliance, UUID correlationId) {
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.deliveryDays = deliveryDays;
        this.paymentTermsDays = paymentTermsDays;
        this.qualityRating = qualityRating;
        this.compliance = compliance;
        this.correlationId = correlationId;
    }
}
