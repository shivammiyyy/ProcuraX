package com.procurax.rfq.repository;

import com.procurax.rfq.domain.RfqItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfqItemRepository extends JpaRepository<RfqItem, UUID> {

    List<RfqItem> findAllByOrganizationIdAndRfqIdOrderByCreatedAt(UUID organizationId, UUID rfqId);

    boolean existsByOrganizationIdAndRfqIdAndId(UUID organizationId, UUID rfqId, UUID itemId);

    void deleteAllByOrganizationIdAndRfqId(UUID organizationId, UUID rfqId);
}
