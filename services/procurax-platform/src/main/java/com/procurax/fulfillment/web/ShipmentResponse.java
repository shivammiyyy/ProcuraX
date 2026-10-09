package com.procurax.fulfillment.web;

import com.procurax.fulfillment.domain.Shipment;
import com.procurax.fulfillment.domain.ShipmentItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID purchaseOrderId,
        String trackingNumber,
        String carrier,
        String status,
        Instant expectedAt,
        Instant shippedAt,
        Instant deliveredAt,
        String exceptionReason,
        List<ShipmentItemResponse> items) {

    public static ShipmentResponse from(Shipment shipment, List<ShipmentItem> items) {
        return new ShipmentResponse(shipment.getId(), shipment.getPurchaseOrderId(),
                shipment.getTrackingNumber(), shipment.getCarrier(), shipment.getStatus(),
                shipment.getExpectedAt(), shipment.getShippedAt(), shipment.getDeliveredAt(),
                shipment.getExceptionReason(), items.stream().map(ShipmentItemResponse::from).toList());
    }
}
