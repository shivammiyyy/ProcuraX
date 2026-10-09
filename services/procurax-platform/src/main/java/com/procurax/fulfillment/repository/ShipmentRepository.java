package com.procurax.fulfillment.repository;

import com.procurax.fulfillment.domain.Shipment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Page<Shipment> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<Shipment> findAllByOrganizationIdAndPurchaseOrderIdOrderByCreatedAt(
            UUID organizationId, UUID purchaseOrderId);

    Optional<Shipment> findByOrganizationIdAndId(UUID organizationId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipment from Shipment shipment "
            + "where shipment.organizationId = :organizationId and shipment.id = :id")
    Optional<Shipment> findForUpdate(@Param("organizationId") UUID organizationId,
                                     @Param("id") UUID id);

    boolean existsByOrganizationIdAndTrackingNumber(UUID organizationId, String trackingNumber);
}
