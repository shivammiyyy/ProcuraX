package com.procurax.purchaseorder.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends BaseEntity {

    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;

    @Column(name = "rfq_item_id", nullable = false, updatable = false)
    private UUID rfqItemId;

    @Column(nullable = false, length = 500, updatable = false)
    private String description;

    @Column(columnDefinition = "text", updatable = false)
    private String specification;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(length = 30, updatable = false)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    protected PurchaseOrderItem() {
        super();
    }

    public PurchaseOrderItem(UUID organizationId, UUID purchaseOrderId, UUID rfqItemId,
                             String description, String specification, int quantity, String unit,
                             BigDecimal unitPrice, BigDecimal lineTotal) {
        super(organizationId);
        this.purchaseOrderId = purchaseOrderId;
        this.rfqItemId = rfqItemId;
        this.description = description;
        this.specification = specification;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public UUID getRfqItemId() { return rfqItemId; }
    public String getDescription() { return description; }
    public String getSpecification() { return specification; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
