package com.procurax.contract.repository;

import com.procurax.contract.domain.ContractAiReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractAiReviewRepository extends JpaRepository<ContractAiReview, UUID> {

    List<ContractAiReview> findAllByOrganizationIdAndContractIdOrderByCreatedAtDesc(
            UUID organizationId, UUID contractId);
    java.util.Optional<ContractAiReview> findByIdAndOrganizationIdAndContractId(
            UUID id, UUID organizationId, UUID contractId);
}

