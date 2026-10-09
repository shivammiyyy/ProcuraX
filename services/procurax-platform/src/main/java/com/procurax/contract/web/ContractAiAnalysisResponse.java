package com.procurax.contract.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.contract.domain.ContractAiAnalysis;
import java.time.Instant;
import java.util.UUID;

public record ContractAiAnalysisResponse(
        UUID id,
        UUID contractId,
        UUID reviewId,
        UUID requestedByUserId,
        Instant createdAt,
        String model,
        JsonNode result) {

    public static ContractAiAnalysisResponse from(ContractAiAnalysis analysis) {
        return new ContractAiAnalysisResponse(analysis.getId(), analysis.getContractId(),
                analysis.getReviewId(), analysis.getCreatedBy(), analysis.getCreatedAt(),
                analysis.getModel(), analysis.getAnalysisResult());
    }
}
