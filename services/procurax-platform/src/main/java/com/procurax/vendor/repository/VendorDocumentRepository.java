package com.procurax.vendor.repository;

import com.procurax.vendor.domain.VendorDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    List<VendorDocument> findAllByOrganizationIdAndVendorIdOrderByCreatedAtDesc(
            UUID organizationId, UUID vendorId);

    Optional<VendorDocument> findByIdAndOrganizationIdAndVendorId(
            UUID id, UUID organizationId, UUID vendorId);
}
