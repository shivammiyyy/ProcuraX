package com.procurax.payment.web;

import com.procurax.payment.domain.PaymentIntent;
import com.procurax.payment.domain.PaymentReceipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID purchaseOrderId,
        UUID mandateId,
        BigDecimal amount,
        String currency,
        String status,
        String sandboxAuthorizationReference,
        Instant authorizedAt,
        Instant capturedAt,
        Instant refundedAt,
        List<PaymentReceiptResponse> receipts) {

    public static PaymentResponse from(PaymentIntent intent, List<PaymentReceipt> receipts) {
        return new PaymentResponse(intent.getId(), intent.getPurchaseOrderId(), intent.getMandateId(),
                intent.getAmount(), intent.getCurrency(), intent.getStatus(),
                intent.getSandboxAuthorizationReference(), intent.getAuthorizedAt(),
                intent.getCapturedAt(), intent.getRefundedAt(),
                receipts.stream().map(PaymentReceiptResponse::from).toList());
    }
}
