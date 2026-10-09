package com.procurax.purchaseorder.repository;

import com.procurax.purchaseorder.domain.PurchaseOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    Page<PurchaseOrder> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<PurchaseOrder> findByOrganizationIdAndId(UUID organizationId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from PurchaseOrder purchaseOrder "
            + "where purchaseOrder.organizationId = :organizationId and purchaseOrder.id = :id")
    Optional<PurchaseOrder> findForPayment(@Param("organizationId") UUID organizationId,
                                          @Param("id") UUID id);

    boolean existsByOrganizationIdAndApprovalRequestId(UUID organizationId, UUID approvalRequestId);

    boolean existsByOrganizationIdAndQuotationId(UUID organizationId, UUID quotationId);
}
