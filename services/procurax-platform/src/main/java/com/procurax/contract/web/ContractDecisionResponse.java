package com.procurax.contract.web;

import com.procurax.contract.domain.ContractDecision;
import java.time.Instant;
import java.util.UUID;

public record ContractDecisionResponse(
        UUID id,
        UUID contractId,
        String decision,
        String comment,
        UUID decidedBy,
        Instant decidedAt) {

    public static ContractDecisionResponse from(ContractDecision decision) {
        return new ContractDecisionResponse(decision.getId(), decision.getContractId(),
                decision.getDecision(), decision.getComment(), decision.getCreatedBy(),
                decision.getDecidedAt());
    }
}
