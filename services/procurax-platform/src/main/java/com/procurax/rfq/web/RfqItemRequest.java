package com.procurax.rfq.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RfqItemRequest(
        @NotBlank @Size(max = 500) String description,
        @Min(1) int quantity,
        @Size(max = 30) String unit,
        @Size(max = 5000) String specification) {
}
