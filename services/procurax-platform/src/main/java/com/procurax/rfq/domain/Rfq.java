package com.procurax.rfq.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rfqs")
public class Rfq extends BaseEntity {

    @Column(name = "intent_id")
    private UUID intentId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "request_type", nullable = false, length = 5)
    private String requestType;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant deadline;

    @Column(name = "delivery_days", nullable = false)
    private int deliveryDays;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "correlation_id")
    private UUID correlationId;

    protected Rfq() {
        super();
    }

    public Rfq(UUID organizationId, String title, String description, String requestType,
               String category, BigDecimal budget, String currency, Instant deadline,
               int deliveryDays, UUID correlationId) {
        super(organizationId);
        this.title = title;
        this.description = description;
        this.requestType = requestType;
        this.category = category;
        this.budget = budget;
        this.currency = currency;
        this.deadline = deadline;
        this.deliveryDays = deliveryDays;
        this.correlationId = correlationId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRequestType() { return requestType; }
    public String getCategory() { return category; }
    public BigDecimal getBudget() { return budget; }
    public String getCurrency() { return currency; }
    public Instant getDeadline() { return deadline; }
    public int getDeliveryDays() { return deliveryDays; }
    public String getStatus() { return status; }
    public UUID getCorrelationId() { return correlationId; }

    public void publish() {
        if (!"DRAFT".equals(status)) {
            throw new IllegalStateException("Only a draft RFQ can be published");
        }
        if (!deadline.isAfter(Instant.now())) {
            throw new IllegalStateException("An RFQ deadline must be in the future");
        }
        status = "PUBLISHED";
    }

    public void close() {
        status = "CLOSED";
    }

    public void markInProgress() {
        if (!"PUBLISHED".equals(status)) {
            throw new IllegalStateException("Only a published RFQ can have a selected quotation");
        }
        status = "IN_PROGRESS";
    }

    public void updateDraft(String title, String description, String requestType, String category,
                            BigDecimal budget, String currency, Instant deadline, int deliveryDays) {
        if (!"DRAFT".equals(status)) {
            throw new IllegalStateException("Only a draft RFQ can be edited");
        }
        this.title = title;
        this.description = description;
        this.requestType = requestType;
        this.category = category;
        this.budget = budget;
        this.currency = currency;
        this.deadline = deadline;
        this.deliveryDays = deliveryDays;
    }
}
