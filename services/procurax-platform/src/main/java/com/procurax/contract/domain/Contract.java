package com.procurax.contract.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contracts")
public class Contract extends BaseEntity {

    @Column(name = "rfq_id", nullable = false, updatable = false)
    private UUID rfqId;

    @Column(name = "quotation_id", nullable = false, updatable = false)
    private UUID quotationId;

    @Column(name = "vendor_id", nullable = false, updatable = false)
    private UUID vendorId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "cloudinary_resource_type", length = 20)
    private String cloudinaryResourceType;

    @Column(name = "cloudinary_delivery_type", length = 20)
    private String cloudinaryDeliveryType;

    @Column(name = "document_file_name", length = 255)
    private String documentFileName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "audit_status", nullable = false, length = 20)
    private String auditStatus = "PENDING";

    protected Contract() {
        super();
    }

    public Contract(UUID organizationId, UUID rfqId, UUID quotationId, UUID vendorId,
                    String content, LocalDate startDate, LocalDate endDate) {
        super(organizationId);
        this.rfqId = rfqId;
        this.quotationId = quotationId;
        this.vendorId = vendorId;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public UUID getRfqId() { return rfqId; }
    public UUID getQuotationId() { return quotationId; }
    public UUID getVendorId() { return vendorId; }
    public String getContent() { return content; }
    public String getFileUrl() { return fileUrl; }
    public String getCloudinaryPublicId() { return cloudinaryPublicId; }
    public String getCloudinaryResourceType() { return cloudinaryResourceType; }
    public String getCloudinaryDeliveryType() { return cloudinaryDeliveryType; }
    public String getDocumentFileName() { return documentFileName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public String getAuditStatus() { return auditStatus; }

    public void markAuditCompleted() {
        this.auditStatus = "COMPLETED";
    }

    public void attachDocument(String url, String publicId, String resourceType,
                               String deliveryType, String fileName) {
        this.fileUrl = url;
        this.cloudinaryPublicId = publicId;
        this.cloudinaryResourceType = resourceType;
        this.cloudinaryDeliveryType = deliveryType;
        this.documentFileName = fileName;
    }

    public void decide(String decision) {
        if (!"DRAFT".equals(status)) {
            throw new IllegalStateException("Only draft contracts can be decided");
        }
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException("Unsupported contract decision");
        }
        this.status = decision;
    }
}
