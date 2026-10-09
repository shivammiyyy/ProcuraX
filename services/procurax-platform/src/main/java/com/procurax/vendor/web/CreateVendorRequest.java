package com.procurax.vendor.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVendorRequest(
        @NotBlank @Size(max = 200) String name,
        @Email @Size(max = 320) String contactEmail,
        @Size(max = 100) String category,
        @Min(0) @Max(365) int paymentTermsDays) {
}
