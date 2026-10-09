package com.procurax.quotation.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record QuotationItemRequest(
        @NotNull UUID rfqItemId,
        @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice,
        @Min(1) int quantity) {
}
