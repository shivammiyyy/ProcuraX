package com.procurax.fulfillment.repository;

import com.procurax.fulfillment.domain.ShipmentItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, UUID> {

    List<ShipmentItem> findAllByOrganizationIdAndShipmentIdOrderByCreatedAt(
            UUID organizationId, UUID shipmentId);

    List<ShipmentItem> findAllByOrganizationIdAndPurchaseOrderId(UUID organizationId, UUID purchaseOrderId);
}
