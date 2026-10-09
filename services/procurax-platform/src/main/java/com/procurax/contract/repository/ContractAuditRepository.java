package com.procurax.contract.repository;

import com.procurax.contract.domain.ContractAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractAuditRepository extends JpaRepository<ContractAudit, UUID> {

    List<ContractAudit> findAllByOrganizationIdAndContractIdOrderByCreatedAtAsc(
            UUID organizationId, UUID contractId);
}
