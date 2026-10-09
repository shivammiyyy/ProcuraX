package com.procurax.payment.web;

import com.procurax.payment.domain.PaymentReceipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceiptResponse(
        UUID id,
        String operation,
        String sandboxReference,
        BigDecimal amount,
        String currency,
        Instant createdAt) {

    public static PaymentReceiptResponse from(PaymentReceipt receipt) {
        return new PaymentReceiptResponse(receipt.getId(), receipt.getOperation(),
                receipt.getSandboxReference(), receipt.getAmount(), receipt.getCurrency(),
                receipt.getCreatedAt());
    }
}
