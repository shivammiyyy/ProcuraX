package com.procurax.rfq.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateRfqRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 10000) String description,
        @NotBlank @Pattern(regexp = "RFQ|RFP") String requestType,
        @NotBlank @Size(max = 100) String category,
        @NotNull @DecimalMin(value = "0.01") BigDecimal budget,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull @Future Instant deadline,
        @Min(1) @Max(365) int deliveryDays,
        @NotEmpty @Size(max = 100) List<@Valid RfqItemRequest> items) {
}
