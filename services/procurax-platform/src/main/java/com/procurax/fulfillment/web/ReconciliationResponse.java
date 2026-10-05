package com.procurax.fulfillment.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.fulfillment.domain.ReconciliationRun;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationResponse(
        UUID id,
        UUID purchaseOrderId,
        UUID invoiceId,
        UUID paymentIntentId,
        String status,
        JsonNode findings,
        Instant reconciledAt) {

    public static ReconciliationResponse from(ReconciliationRun run) {
        return new ReconciliationResponse(run.getId(), run.getPurchaseOrderId(),
                run.getInvoiceId(), run.getPaymentIntentId(), run.getStatus(),
                run.getFindings(), run.getReconciledAt());
    }
}
