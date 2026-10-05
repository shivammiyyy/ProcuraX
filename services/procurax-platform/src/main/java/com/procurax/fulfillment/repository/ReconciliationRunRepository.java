package com.procurax.fulfillment.repository;

import com.procurax.fulfillment.domain.ReconciliationRun;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, UUID> {

    Page<ReconciliationRun> findAllByOrganizationIdOrderByReconciledAtDesc(
            UUID organizationId, Pageable pageable);

    Optional<ReconciliationRun> findByOrganizationIdAndId(UUID organizationId, UUID id);
}
