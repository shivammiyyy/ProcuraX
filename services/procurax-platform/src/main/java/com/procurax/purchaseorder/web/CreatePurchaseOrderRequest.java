package com.procurax.purchaseorder.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreatePurchaseOrderRequest(@NotNull UUID approvalRequestId, @NotNull UUID quotationId) {
}
