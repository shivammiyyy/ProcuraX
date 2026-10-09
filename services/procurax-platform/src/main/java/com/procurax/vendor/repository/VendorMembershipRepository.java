package com.procurax.vendor.repository;

import com.procurax.vendor.domain.VendorMembership;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorMembershipRepository extends JpaRepository<VendorMembership, UUID> {

    boolean existsByOrganizationIdAndVendorIdAndUserId(UUID organizationId, UUID vendorId, UUID userId);

    List<VendorMembership> findAllByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndVendorId(UUID organizationId, UUID vendorId);
}
