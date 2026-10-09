package com.procurax.quotation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.procurax.quotation.domain.Quotation;
import com.procurax.quotation.domain.VendorScore;
import com.procurax.vendor.domain.Vendor;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Calculates the official quote score deterministically; no LLM or client-provided score is used. */
@Service
public class VendorScoringService {

    private final VendorScoringProperties properties;
    private final ObjectMapper objectMapper;

    public VendorScoringService(VendorScoringProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void validateWeights() {
        properties.validate();
    }

    public VendorScore score(UUID organizationId, Quotation quote, Vendor vendor,
                             int targetDeliveryDays, BigDecimal budget) {
        JsonNode compliance = quote.getCompliance();
        Map<String, BigDecimal> factors = Map.of(
                "price", priceScore(quote.getTotalAmount(), budget),
                "delivery", deliveryScore(quote.getDeliveryDays(), targetDeliveryDays),
                "quality", quote.getQualityRating().multiply(BigDecimal.valueOf(20)),
                "compliance", complianceScore(compliance),
                "performance", vendor.getPerformanceScore(),
                "paymentTerms", paymentTermsScore(quote.getPaymentTermsDays()));

        BigDecimal weighted = factors.entrySet().stream()
                .map(entry -> entry.getValue().multiply(properties.asMap().get(entry.getKey())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new VendorScore(organizationId, quote.getId(), vendor.getId(), weighted,
                toJson(factors), toJson(properties.asMap()), quote.getCorrelationId());
    }

    private BigDecimal priceScore(BigDecimal amount, BigDecimal budget) {
        BigDecimal score = BigDecimal.ONE.subtract(amount.divide(budget, 6, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100));
        return clamp(score);
    }

    private BigDecimal deliveryScore(int actualDays, int targetDays) {
        BigDecimal ratio = BigDecimal.valueOf(actualDays)
                .divide(BigDecimal.valueOf(targetDays), 6, RoundingMode.HALF_UP);
        return clamp(BigDecimal.ONE.subtract(ratio.subtract(BigDecimal.ONE).max(BigDecimal.ZERO))
                .multiply(BigDecimal.valueOf(100)));
    }

    private BigDecimal complianceScore(JsonNode compliance) {
        BigDecimal iso = boolScore(compliance, "isoCertification");
        BigDecimal material = switch (compliance.path("materialGrade").asText("")) {
            case "A+" -> BigDecimal.valueOf(100);
            case "A" -> BigDecimal.valueOf(80);
            case "B" -> BigDecimal.valueOf(60);
            case "C" -> BigDecimal.valueOf(40);
            default -> BigDecimal.ZERO;
        };
        BigDecimal environment = boolScore(compliance, "environmentalStandards");
        BigDecimal documents = boolScore(compliance, "documentSubmission");
        return iso.multiply(new BigDecimal("0.35"))
                .add(material.multiply(new BigDecimal("0.25")))
                .add(environment.multiply(new BigDecimal("0.20")))
                .add(documents.multiply(new BigDecimal("0.20")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal boolScore(JsonNode node, String key) {
        return node.path(key).asBoolean(false) ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
    }

    private BigDecimal paymentTermsScore(int days) {
        return BigDecimal.valueOf(Math.max(0, Math.min(100, 70 + days)));
    }

    private BigDecimal clamp(BigDecimal score) {
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private JsonNode toJson(Object values) {
        return objectMapper.valueToTree(values);
    }
}
