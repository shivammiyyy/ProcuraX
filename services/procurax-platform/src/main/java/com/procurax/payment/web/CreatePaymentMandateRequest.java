package com.procurax.payment.web;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreatePaymentMandateRequest(
        @NotNull UUID purchaseOrderId,
        @NotNull @Future Instant expiresAt) {
}
