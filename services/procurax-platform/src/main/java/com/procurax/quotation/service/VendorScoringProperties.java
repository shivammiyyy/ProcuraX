package com.procurax.quotation.service;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@ConfigurationProperties(prefix = "procurax.vendor-scoring.weights")
public class VendorScoringProperties {

    private BigDecimal price = new BigDecimal("0.30");
    private BigDecimal delivery = new BigDecimal("0.20");
    private BigDecimal quality = new BigDecimal("0.20");
    private BigDecimal compliance = new BigDecimal("0.15");
    private BigDecimal performance = new BigDecimal("0.10");
    private BigDecimal paymentTerms = new BigDecimal("0.05");

    public void validate() {
        Map<String, BigDecimal> weights = asMap();
        Assert.isTrue(weights.values().stream().allMatch(v -> v != null && v.signum() >= 0),
                "Vendor scoring weights must be non-negative");
        BigDecimal total = weights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Assert.isTrue(total.compareTo(BigDecimal.ONE) == 0, "Vendor scoring weights must sum to 1.0");
    }

    public Map<String, BigDecimal> asMap() {
        return Map.of(
                "price", price,
                "delivery", delivery,
                "quality", quality,
                "compliance", compliance,
                "performance", performance,
                "paymentTerms", paymentTerms);
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getDelivery() { return delivery; }
    public void setDelivery(BigDecimal delivery) { this.delivery = delivery; }
    public BigDecimal getQuality() { return quality; }
    public void setQuality(BigDecimal quality) { this.quality = quality; }
    public BigDecimal getCompliance() { return compliance; }
    public void setCompliance(BigDecimal compliance) { this.compliance = compliance; }
    public BigDecimal getPerformance() { return performance; }
    public void setPerformance(BigDecimal performance) { this.performance = performance; }
    public BigDecimal getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(BigDecimal paymentTerms) { this.paymentTerms = paymentTerms; }
}
