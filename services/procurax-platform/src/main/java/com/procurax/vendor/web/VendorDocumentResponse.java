package com.procurax.vendor.web;

import com.procurax.vendor.domain.VendorDocument;
import com.procurax.vendor.domain.VendorDocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record VendorDocumentResponse(
        UUID id,
        UUID vendorId,
        String documentType,
        String fileName,
        String storageUrl,
        VendorDocumentStatus verificationStatus,
        UUID verifiedBy,
        Instant verifiedAt,
        String rejectionReason,
        Instant createdAt) {

    public static VendorDocumentResponse from(VendorDocument document) {
        return new VendorDocumentResponse(document.getId(), document.getVendorId(),
                document.getDocumentType(), document.getFileName(), document.getStorageUrl(),
                document.getVerificationStatus(), document.getVerifiedBy(), document.getVerifiedAt(),
                document.getRejectionReason(), document.getCreatedAt());
    }
}
