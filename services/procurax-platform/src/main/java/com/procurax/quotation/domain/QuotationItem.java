package com.procurax.quotation.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quotation_items")
public class QuotationItem extends BaseEntity {

    @Column(name = "quotation_id", nullable = false, updatable = false)
    private UUID quotationId;

    @Column(name = "rfq_item_id")
    private UUID rfqItemId;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected QuotationItem() {
        super();
    }

    public QuotationItem(UUID organizationId, UUID quotationId, UUID rfqItemId,
                         BigDecimal unitPrice, int quantity) {
        super(organizationId);
        this.quotationId = quotationId;
        this.rfqItemId = rfqItemId;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public UUID getQuotationId() { return quotationId; }
    public UUID getRfqItemId() { return rfqItemId; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
