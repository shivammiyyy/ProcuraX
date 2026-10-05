package com.procurax.vendor.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_documents")
public class VendorDocument extends BaseEntity {

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_url", nullable = false, length = 1000)
    private String storageUrl;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "cloudinary_resource_type", length = 20)
    private String cloudinaryResourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VendorDocumentStatus verificationStatus = VendorDocumentStatus.UNVERIFIED;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    protected VendorDocument() {
        super();
    }

    public VendorDocument(UUID organizationId, UUID vendorId, String documentType,
                          String fileName, String storageUrl) {
        super(organizationId);
        this.vendorId = vendorId;
        this.documentType = documentType;
        this.fileName = fileName;
        this.storageUrl = storageUrl;
    }

    public UUID getVendorId() { return vendorId; }
    public String getDocumentType() { return documentType; }
    public String getFileName() { return fileName; }
    public String getStorageUrl() { return storageUrl; }
    public String getCloudinaryPublicId() { return cloudinaryPublicId; }
    public String getCloudinaryResourceType() { return cloudinaryResourceType; }
    public VendorDocumentStatus getVerificationStatus() { return verificationStatus; }
    public UUID getVerifiedBy() { return verifiedBy; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public String getRejectionReason() { return rejectionReason; }

    public void setCloudinaryAsset(String publicId, String resourceType) {
        this.cloudinaryPublicId = publicId;
        this.cloudinaryResourceType = resourceType;
    }

    public void verify(VendorDocumentStatus status, UUID actorId, String reason) {
        this.verificationStatus = status;
        this.verifiedBy = actorId;
        this.verifiedAt = Instant.now();
        this.rejectionReason = reason;
    }
}
