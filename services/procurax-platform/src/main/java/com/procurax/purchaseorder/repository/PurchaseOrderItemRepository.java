package com.procurax.purchaseorder.repository;

import com.procurax.purchaseorder.domain.PurchaseOrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, UUID> {

    List<PurchaseOrderItem> findAllByOrganizationIdAndPurchaseOrderIdOrderByCreatedAt(
            UUID organizationId, UUID purchaseOrderId);
}
