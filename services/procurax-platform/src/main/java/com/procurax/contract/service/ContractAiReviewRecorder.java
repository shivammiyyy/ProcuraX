package com.procurax.contract.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.contract.domain.Contract;
import com.procurax.contract.domain.ContractAiAnalysis;
import com.procurax.contract.domain.ContractAiReview;
import com.procurax.contract.repository.ContractAiAnalysisRepository;
import com.procurax.contract.repository.ContractAiReviewRepository;
import com.procurax.contract.web.ContractEmbeddingResponse;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.outbox.OutboxEventWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractAiReviewRecorder {

    private final ContractAiReviewRepository reviewRepository;
    private final ContractAiAnalysisRepository analysisRepository;
    private final JdbcTemplate jdbc;
    private final OrganizationContext organizationContext;
    private final OutboxEventWriter eventWriter;

    public ContractAiReviewRecorder(ContractAiReviewRepository reviewRepository,
                                    ContractAiAnalysisRepository analysisRepository,
                                    JdbcTemplate jdbc,
                                    OrganizationContext organizationContext,
                                    OutboxEventWriter eventWriter) {
        this.reviewRepository = reviewRepository;
        this.analysisRepository = analysisRepository;
        this.jdbc = jdbc;
        this.organizationContext = organizationContext;
        this.eventWriter = eventWriter;
    }

    @Transactional
    public ContractAiReview record(Contract contract, UUID actorId, JsonNode result,
                                   ContractEmbeddingResponse embeddings) {
        ContractAiReview review = new ContractAiReview(contract.getOrganizationId(),
                contract.getId(), result, "ephemeral_bm25");
        review.setCreatedBy(actorId);
        review.setUpdatedBy(actorId);
        ContractAiReview saved = reviewRepository.saveAndFlush(review);
        List<Object[]> chunks = embeddings.chunks().stream()
                .map(chunk -> new Object[] {
                        UUID.randomUUID(), contract.getOrganizationId(), contract.getId(), saved.getId(),
                        chunk.chunkId(), chunk.startCharacter(), chunk.endCharacter(), chunk.text(),
                        embeddings.model(), ContractVector.toLiteral(chunk.embedding()), actorId, actorId
                })
                .toList();
        jdbc.batchUpdate("""
                INSERT INTO contract_ai_review_chunks
                    (id, organization_id, contract_id, review_id, chunk_index, start_character,
                     end_character, content, embedding_model, embedding, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?)
                """, chunks);
        eventWriter.record("CONTRACT", contract.getId(), contract.getOrganizationId(),
                "CONTRACT_AI_REVIEW_RECORDED", "procurax.contract.v1",
                OutboxEventWriter.currentCorrelationId(contract.getId()), actorId,
                Map.of("contractAiReviewId", saved.getId(), "retrievalMethod", saved.getRetrievalMethod(),
                        "chunkCount", chunks.size(), "embeddingModel", embeddings.model()));
        return saved;
    }

    @Transactional
    public ContractAiAnalysis recordAnalysis(Contract contract, ContractAiReview review, UUID actorId,
                                             JsonNode result) {
        ContractAiAnalysis analysis = new ContractAiAnalysis(contract.getOrganizationId(), contract.getId(),
                review.getId(), result.path("model").asText(), result);
        analysis.setCreatedBy(actorId);
        analysis.setUpdatedBy(actorId);
        ContractAiAnalysis saved = analysisRepository.saveAndFlush(analysis);
        eventWriter.record("CONTRACT", contract.getId(), contract.getOrganizationId(),
                "CONTRACT_AI_ANALYSIS_RECORDED", "procurax.contract.v1",
                OutboxEventWriter.currentCorrelationId(contract.getId()), actorId,
                Map.of("contractAiAnalysisId", saved.getId(), "contractAiReviewId", review.getId(),
                        "model", saved.getModel()));
        return saved;
    }
}
