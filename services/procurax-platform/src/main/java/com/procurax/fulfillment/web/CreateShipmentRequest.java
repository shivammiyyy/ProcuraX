package com.procurax.fulfillment.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateShipmentRequest(
        @NotNull UUID purchaseOrderId,
        @NotBlank @Size(max = 100) String trackingNumber,
        @NotBlank @Size(max = 120) String carrier,
        @FutureOrPresent Instant expectedAt,
        @NotEmpty @Size(max = 100) List<@Valid ShipmentItemRequest> items) {
}
