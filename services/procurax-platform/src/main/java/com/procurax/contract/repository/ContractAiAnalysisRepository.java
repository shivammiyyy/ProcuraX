package com.procurax.contract.repository;

import com.procurax.contract.domain.ContractAiAnalysis;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractAiAnalysisRepository extends JpaRepository<ContractAiAnalysis, UUID> {

    List<ContractAiAnalysis> findAllByOrganizationIdAndContractIdOrderByCreatedAtDesc(
            UUID organizationId, UUID contractId);
}
