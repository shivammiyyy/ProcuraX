package com.procurax.quotation.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateQuotationRequest(
        @NotNull UUID rfqId,
        UUID vendorId,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @Min(1) @Max(365) int deliveryDays,
        @Min(0) @Max(365) int paymentTermsDays,
        @NotNull @DecimalMin(value = "0.00") @DecimalMax(value = "5.00") BigDecimal qualityRating,
        @NotNull JsonNode compliance,
        @NotEmpty @Size(max = 100) List<@Valid QuotationItemRequest> items) {
}
