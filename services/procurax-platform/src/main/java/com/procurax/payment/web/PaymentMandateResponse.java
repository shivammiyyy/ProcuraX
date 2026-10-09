package com.procurax.payment.web;

import com.procurax.payment.domain.PaymentMandate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentMandateResponse(
        UUID id,
        UUID purchaseOrderId,
        BigDecimal maximumAmount,
        String currency,
        Instant expiresAt,
        String status,
        Instant createdAt,
        long version) {

    public static PaymentMandateResponse from(PaymentMandate mandate) {
        return new PaymentMandateResponse(mandate.getId(), mandate.getPurchaseOrderId(),
                mandate.getMaximumAmount(), mandate.getCurrency(), mandate.getExpiresAt(),
                mandate.getStatus(), mandate.getCreatedAt(), mandate.getVersion());
    }
}
