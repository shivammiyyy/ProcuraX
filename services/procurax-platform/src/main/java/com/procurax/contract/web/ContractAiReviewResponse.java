package com.procurax.contract.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.contract.domain.ContractAiReview;
import java.time.Instant;
import java.util.UUID;

public record ContractAiReviewResponse(
        UUID id,
        UUID contractId,
        UUID requestedByUserId,
        Instant createdAt,
        String retrievalMethod,
        JsonNode result) {

    public static ContractAiReviewResponse from(ContractAiReview review) {
        return new ContractAiReviewResponse(review.getId(), review.getContractId(),
                review.getCreatedBy(), review.getCreatedAt(), review.getRetrievalMethod(),
                review.getReviewResult());
    }
}
