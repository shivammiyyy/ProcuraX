package com.procurax.vendor.web;

import com.procurax.vendor.domain.Vendor;
import java.math.BigDecimal;
import java.util.UUID;

public record VendorSummary(
        UUID id,
        String name,
        String contactEmail,
        String category,
        String status,
        String riskLevel,
        int paymentTermsDays,
        BigDecimal performanceScore) {

    public static VendorSummary from(Vendor vendor) {
        return new VendorSummary(vendor.getId(), vendor.getName(), vendor.getContactEmail(),
                vendor.getCategory(), vendor.getStatus(), vendor.getRiskLevel(),
                vendor.getPaymentTermsDays(), vendor.getPerformanceScore());
    }
}
