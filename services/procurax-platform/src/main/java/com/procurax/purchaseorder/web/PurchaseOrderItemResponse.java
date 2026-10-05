package com.procurax.purchaseorder.web;

import com.procurax.purchaseorder.domain.PurchaseOrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderItemResponse(
        UUID id,
        UUID rfqItemId,
        String description,
        String specification,
        int quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static PurchaseOrderItemResponse from(PurchaseOrderItem item) {
        return new PurchaseOrderItemResponse(item.getId(), item.getRfqItemId(), item.getDescription(),
                item.getSpecification(), item.getQuantity(), item.getUnit(),
                item.getUnitPrice(), item.getLineTotal());
    }
}
