package com.procurax.fulfillment.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateReconciliationRequest(@NotNull UUID invoiceId) {
}
