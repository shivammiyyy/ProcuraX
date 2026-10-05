package com.procurax.fulfillment.web;

import com.procurax.fulfillment.domain.Invoice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID purchaseOrderId,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        long version) {

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getPurchaseOrderId(),
                invoice.getInvoiceNumber(), invoice.getInvoiceDate(), invoice.getDueDate(),
                invoice.getAmount(), invoice.getCurrency(), invoice.getStatus(),
                invoice.getCreatedAt(), invoice.getVersion());
    }
}
