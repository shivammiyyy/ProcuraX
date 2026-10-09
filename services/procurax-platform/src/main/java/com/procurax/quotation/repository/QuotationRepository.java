package com.procurax.quotation.repository;

import com.procurax.quotation.domain.Quotation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    Page<Quotation> findAllByOrganizationIdAndVendorId(UUID organizationId, UUID vendorId, Pageable pageable);

    Page<Quotation> findAllByOrganizationIdAndVendorIdIn(UUID organizationId, Collection<UUID> vendorIds,
                                                          Pageable pageable);

    Page<Quotation> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    List<Quotation> findAllByOrganizationIdAndRfqIdOrderByCreatedAt(UUID organizationId, UUID rfqId);

    List<Quotation> findAllByOrganizationIdAndRfqIdAndVendorId(UUID organizationId, UUID rfqId, UUID vendorId);

    Optional<Quotation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quotation from Quotation quotation "
            + "where quotation.id = :id and quotation.organizationId = :organizationId")
    Optional<Quotation> findForPurchaseOrder(@Param("id") UUID id,
                                             @Param("organizationId") UUID organizationId);

    boolean existsByOrganizationIdAndRfqIdAndVendorId(UUID organizationId, UUID rfqId, UUID vendorId);

    boolean existsByOrganizationIdAndRfqIdAndStatus(UUID organizationId, UUID rfqId, String status);

    boolean existsByOrganizationIdAndRfqIdAndVendorIdAndStatus(
            UUID organizationId, UUID rfqId, UUID vendorId, String status);

    long countByOrganizationIdAndRfqId(UUID organizationId, UUID rfqId);

    List<Quotation> findAllByOrganizationIdAndVendorIdOrderByCreatedAt(UUID organizationId, UUID vendorId);

    List<Quotation> findAllByOrganizationIdAndRfqIdAndVendorIdInOrderByCreatedAt(
            UUID organizationId, UUID rfqId, Collection<UUID> vendorIds);
}
