package com.procurax.fulfillment.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "shipment_items")
public class ShipmentItem extends BaseEntity {

    @Column(name = "shipment_id", nullable = false, updatable = false)
    private UUID shipmentId;

    @Column(name = "purchase_order_id", nullable = false, updatable = false)
    private UUID purchaseOrderId;

    @Column(name = "purchase_order_item_id", nullable = false, updatable = false)
    private UUID purchaseOrderItemId;

    @Column(nullable = false, length = 500, updatable = false)
    private String description;

    @Column(nullable = false, updatable = false)
    private int quantity;

    protected ShipmentItem() {
        super();
    }

    public ShipmentItem(UUID organizationId, UUID shipmentId, UUID purchaseOrderId,
                        UUID purchaseOrderItemId, String description, int quantity) {
        super(organizationId);
        this.shipmentId = shipmentId;
        this.purchaseOrderId = purchaseOrderId;
        this.purchaseOrderItemId = purchaseOrderItemId;
        this.description = description;
        this.quantity = quantity;
    }

    public UUID getShipmentId() { return shipmentId; }
    public UUID getPurchaseOrderId() { return purchaseOrderId; }
    public UUID getPurchaseOrderItemId() { return purchaseOrderItemId; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
}
