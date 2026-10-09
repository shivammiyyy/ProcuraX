package com.procurax.quotation.repository;

import com.procurax.quotation.domain.VendorScore;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorScoreRepository extends JpaRepository<VendorScore, UUID> {

    List<VendorScore> findAllByOrganizationIdAndQuotationId(UUID organizationId, UUID quotationId);

    void deleteAllByOrganizationIdAndQuotationId(UUID organizationId, UUID quotationId);
}
