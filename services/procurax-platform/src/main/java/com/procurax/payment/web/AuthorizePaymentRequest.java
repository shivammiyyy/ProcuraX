package com.procurax.payment.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AuthorizePaymentRequest(
        @NotNull UUID purchaseOrderId,
        @NotNull UUID mandateId) {
}
