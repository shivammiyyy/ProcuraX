package com.procurax.contract.web;

import java.util.List;
import java.util.UUID;

public record ContractSemanticSearchResponse(
        UUID contractId,
        String embeddingModel,
        List<Match> matches) {

    public record Match(
            UUID reviewId,
            int chunkId,
            int startCharacter,
            int endCharacter,
            String excerpt,
            double cosineSimilarity) {
    }
}
