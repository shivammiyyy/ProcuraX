package com.procurax.fulfillment.web;

import com.procurax.fulfillment.domain.ShipmentItem;
import java.util.UUID;

public record ShipmentItemResponse(UUID id, UUID purchaseOrderItemId, String description, int quantity) {

    public static ShipmentItemResponse from(ShipmentItem item) {
        return new ShipmentItemResponse(item.getId(), item.getPurchaseOrderItemId(),
                item.getDescription(), item.getQuantity());
    }
}
