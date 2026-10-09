package com.procurax.quotation.repository;

import com.procurax.quotation.domain.QuotationItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationItemRepository extends JpaRepository<QuotationItem, UUID> {

    List<QuotationItem> findAllByOrganizationIdAndQuotationIdOrderByCreatedAt(UUID organizationId, UUID quotationId);
}
