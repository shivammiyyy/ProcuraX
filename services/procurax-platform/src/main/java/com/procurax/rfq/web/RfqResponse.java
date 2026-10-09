package com.procurax.rfq.web;

import com.procurax.rfq.domain.Rfq;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RfqResponse(
        UUID id,
        String title,
        String description,
        String requestType,
        String category,
        BigDecimal budget,
        String currency,
        Instant deadline,
        int deliveryDays,
        String status,
        UUID correlationId,
        List<RfqItemResponse> items) {

    public static RfqResponse from(Rfq rfq, List<RfqItemResponse> items) {
        return new RfqResponse(rfq.getId(), rfq.getTitle(), rfq.getDescription(), rfq.getRequestType(),
                rfq.getCategory(), rfq.getBudget(), rfq.getCurrency(), rfq.getDeadline(),
                rfq.getDeliveryDays(), rfq.getStatus(), rfq.getCorrelationId(), items);
    }
}
