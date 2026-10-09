package com.procurax.contract.web;

import java.util.List;
import java.util.UUID;

public record ContractEmbeddingResponse(
        UUID contractId,
        UUID organizationId,
        UUID requestedByUserId,
        String model,
        int dimensions,
        List<Chunk> chunks) {

    public record Chunk(
            int chunkId,
            int startCharacter,
            int endCharacter,
            String text,
            List<Double> embedding) {
    }
}
