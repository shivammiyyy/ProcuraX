package com.procurax.quotation.web;

import com.procurax.quotation.domain.QuotationItem;
import java.math.BigDecimal;
import java.util.UUID;

public record QuotationItemResponse(UUID rfqItemId, BigDecimal unitPrice, int quantity) {

    public static QuotationItemResponse from(QuotationItem item) {
        return new QuotationItemResponse(item.getRfqItemId(), item.getUnitPrice(), item.getQuantity());
    }
}
