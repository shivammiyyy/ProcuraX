package com.procurax.contract.repository;

import com.procurax.contract.domain.ContractDecision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractDecisionRepository extends JpaRepository<ContractDecision, UUID> {

    List<ContractDecision> findAllByOrganizationIdAndContractIdOrderByDecidedAtDesc(
            UUID organizationId, UUID contractId);
}
