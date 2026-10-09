package com.procurax.vendor.web;

import com.procurax.vendor.domain.VendorDocumentStatus;
import jakarta.validation.constraints.Size;

public record VerifyVendorDocumentRequest(
        VendorDocumentStatus status,
        @Size(max = 1000) String rejectionReason) {
}
