package com.procurax.vendor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_memberships")
public class VendorMembership {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "vendor_id", nullable = false, updatable = false)
    private UUID vendorId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    protected VendorMembership() {
    }

    public VendorMembership(UUID organizationId, UUID vendorId, UUID userId, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.vendorId = vendorId;
        this.userId = userId;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getVendorId() {
        return vendorId;
    }
}
