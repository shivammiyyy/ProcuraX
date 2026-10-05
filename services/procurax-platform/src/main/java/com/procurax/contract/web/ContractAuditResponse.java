package com.procurax.contract.web;

import com.procurax.contract.domain.ContractAudit;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContractAuditResponse(
        UUID id,
        UUID contractId,
        String riskLevel,
        String finding,
        String clause,
        String explanation,
        String recommendation,
        BigDecimal confidence,
        UUID createdBy,
        Instant createdAt) {

    public static ContractAuditResponse from(ContractAudit audit) {
        return new ContractAuditResponse(audit.getId(), audit.getContractId(), audit.getRiskLevel(),
                audit.getFinding(), audit.getClause(), audit.getExplanation(), audit.getRecommendation(),
                audit.getConfidence(), audit.getCreatedBy(), audit.getCreatedAt());
    }
}
