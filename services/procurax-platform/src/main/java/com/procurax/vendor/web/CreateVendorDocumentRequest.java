package com.procurax.vendor.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateVendorDocumentRequest(
        @NotBlank
        @Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}")
        String documentType,
        @NotBlank
        @Size(max = 255)
        String fileName,
        @NotBlank
        @Size(max = 1000)
        String storageUrl) {
}
