package com.procurax.fulfillment.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShipmentStatusRequest(
        @NotBlank @Pattern(regexp = "DELIVERED|EXCEPTION") String status,
        @Size(max = 1000) String exceptionReason) {
}
