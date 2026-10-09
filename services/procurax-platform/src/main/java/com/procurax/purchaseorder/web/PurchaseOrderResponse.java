package com.procurax.purchaseorder.web;

import com.procurax.purchaseorder.domain.PurchaseOrder;
import com.procurax.purchaseorder.domain.PurchaseOrderItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
        UUID id,
        String poNumber,
        UUID approvalRequestId,
        UUID rfqId,
        UUID quotationId,
        UUID vendorId,
        String vendorName,
        String vendorContactEmail,
        BigDecimal totalAmount,
        String currency,
        int deliveryDays,
        int paymentTermsDays,
        String status,
        Instant issuedAt,
        Instant createdAt,
        long version,
        List<PurchaseOrderItemResponse> items) {

    public static PurchaseOrderResponse from(PurchaseOrder order, List<PurchaseOrderItem> items) {
        return new PurchaseOrderResponse(order.getId(), order.getPoNumber(),
                order.getApprovalRequestId(), order.getRfqId(), order.getQuotationId(), order.getVendorId(),
                order.getVendorName(), order.getVendorContactEmail(), order.getTotalAmount(),
                order.getCurrency(), order.getDeliveryDays(), order.getPaymentTermsDays(),
                order.getStatus(), order.getIssuedAt(), order.getCreatedAt(), order.getVersion(),
                items.stream().map(PurchaseOrderItemResponse::from).toList());
    }
}
