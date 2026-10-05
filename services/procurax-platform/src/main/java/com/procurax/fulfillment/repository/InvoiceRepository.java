package com.procurax.fulfillment.repository;

import com.procurax.fulfillment.domain.Invoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<Invoice> findByOrganizationIdAndId(UUID organizationId, UUID id);

    Optional<Invoice> findByOrganizationIdAndPurchaseOrderId(UUID organizationId, UUID purchaseOrderId);

    boolean existsByOrganizationIdAndInvoiceNumberIgnoreCase(UUID organizationId, String invoiceNumber);
}
