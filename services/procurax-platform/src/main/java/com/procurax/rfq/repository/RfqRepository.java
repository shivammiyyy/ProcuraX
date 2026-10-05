package com.procurax.rfq.repository;

import com.procurax.rfq.domain.Rfq;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfqRepository extends JpaRepository<Rfq, UUID> {

    Page<Rfq> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Rfq> findAllByOrganizationIdAndStatus(UUID organizationId, String status, Pageable pageable);

    Optional<Rfq> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
