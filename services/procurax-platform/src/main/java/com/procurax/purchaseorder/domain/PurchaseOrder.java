package com.procurax.purchaseorder.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseEntity {

    @Column(name = "po_number", nullable = false, length = 40, updatable = false)
    private String poNumber;

    @Column(name = "approval_request_id", nullable = false, updatable = false)
    private UUID approvalRequestId;

    @Column(name = "rfq_id", nullable = false, updatable = false)
    private UUID rfqId;

    @Column(name = "quotation_id", nullable = false, updatable = false)
    private UUID quotationId;

    @Column(name = "vendor_id", nullable = false, updatable = false)
    private UUID vendorId;

    @Column(name = "vendor_name", nullable = false, length = 200, updatable = false)
    private String vendorName;

    @Column(name = "vendor_contact_email", length = 320, updatable = false)
    private String vendorContactEmail;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "delivery_days", nullable = false, updatable = false)
    private int deliveryDays;

    @Column(name = "payment_terms_days", nullable = false, updatable = false)
    private int paymentTermsDays;

    @Column(nullable = false, length = 20)
    private String status = "ISSUED";

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    protected PurchaseOrder() {
        super();
    }

    public PurchaseOrder(UUID organizationId, String poNumber, UUID approvalRequestId, UUID rfqId,
                         UUID quotationId, UUID vendorId, String vendorName, String vendorContactEmail,
                         BigDecimal totalAmount, String currency, int deliveryDays, int paymentTermsDays) {
        super(organizationId);
        this.poNumber = poNumber;
        this.approvalRequestId = approvalRequestId;
        this.rfqId = rfqId;
        this.quotationId = quotationId;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.vendorContactEmail = vendorContactEmail;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.deliveryDays = deliveryDays;
        this.paymentTermsDays = paymentTermsDays;
    }

    public String getPoNumber() { return poNumber; }
    public UUID getApprovalRequestId() { return approvalRequestId; }
    public UUID getRfqId() { return rfqId; }
    public UUID getQuotationId() { return quotationId; }
    public UUID getVendorId() { return vendorId; }
    public String getVendorName() { return vendorName; }
    public String getVendorContactEmail() { return vendorContactEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public int getDeliveryDays() { return deliveryDays; }
    public int getPaymentTermsDays() { return paymentTermsDays; }
    public String getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
}
