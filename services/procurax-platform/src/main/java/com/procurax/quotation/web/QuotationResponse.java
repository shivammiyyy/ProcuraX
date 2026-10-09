package com.procurax.quotation.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.quotation.domain.Quotation;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuotationResponse(
        UUID id,
        UUID rfqId,
        UUID vendorId,
        BigDecimal totalAmount,
        String currency,
        int deliveryDays,
        int paymentTermsDays,
        BigDecimal qualityRating,
        JsonNode compliance,
        String status,
        UUID correlationId,
        List<QuotationItemResponse> items,
        VendorScoreResponse vendorScore) {

    public static QuotationResponse from(Quotation quotation, List<QuotationItemResponse> items,
                                         VendorScoreResponse score) {
        return new QuotationResponse(quotation.getId(), quotation.getRfqId(), quotation.getVendorId(),
                quotation.getTotalAmount(), quotation.getCurrency(), quotation.getDeliveryDays(),
                quotation.getPaymentTermsDays(), quotation.getQualityRating(), quotation.getCompliance(),
                quotation.getStatus(), quotation.getCorrelationId(), items, score);
    }
}
