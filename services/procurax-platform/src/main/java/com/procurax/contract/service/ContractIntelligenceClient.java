package com.procurax.contract.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.error.BusinessException;
import com.procurax.contract.web.ContractEmbeddingResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.net.http.HttpClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ContractIntelligenceClient {

    private final RestClient restClient;
    private final String serviceToken;
    private final String embeddingModel;
    private final int embeddingDimensions;

    public ContractIntelligenceClient(RestClient.Builder builder,
                                      @Value("${procurax.ai.base-url:http://localhost:8001}") String baseUrl,
                                      @Value("${procurax.ai.service-token:}") String serviceToken,
                                      @Value("${procurax.ai.embedding-model:qwen3-embedding:0.6b}") String embeddingModel,
                                      @Value("${procurax.ai.embedding-dimensions:1024}") int embeddingDimensions) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(150));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.serviceToken = serviceToken;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
    }

    public JsonNode review(UUID contractId, String contractText, UUID organizationId, UUID actorId) {
        requireConfigured();
        try {
            JsonNode result = restClient.post()
                    .uri("/internal/v1/contracts/{contractId}/review", contractId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + serviceToken)
                    .header("X-ProcuraX-Organization-Id", organizationId.toString())
                    .header("X-ProcuraX-Actor-Id", actorId.toString())
                    .body(Map.of("contract_text", contractText))
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null
                    || !contractId.toString().equals(result.path("contract_id").asText())
                    || !organizationId.toString().equals(result.path("organization_id").asText())
                    || !actorId.toString().equals(result.path("requested_by_user_id").asText())
                    || !"ephemeral_bm25".equals(result.path("retrieval_method").asText())
                    || !result.path("requires_human_review").asBoolean(false)
                    || result.path("document_persisted").asBoolean(true)) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_INTELLIGENCE_RESPONSE",
                        "Contract intelligence returned an invalid review response");
            }
            return result;
        } catch (RestClientException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "CONTRACT_INTELLIGENCE_FAILED",
                    "Contract intelligence could not review the draft", exception);
        }
    }

    public String extractText(byte[] content, UUID organizationId, UUID actorId) {
        requireConfigured();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("document", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "document";
            }
        });
        try {
            JsonNode result = restClient.post()
                    .uri("/internal/v1/contracts/extract-text")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("Authorization", "Bearer " + serviceToken)
                    .header("X-ProcuraX-Organization-Id", organizationId.toString())
                    .header("X-ProcuraX-Actor-Id", actorId.toString())
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 413 || status.value() == 422, (request, response) -> {
                        HttpStatus responseStatus = response.getStatusCode().value() == 413
                                ? HttpStatus.PAYLOAD_TOO_LARGE
                                : HttpStatus.UNPROCESSABLE_ENTITY;
                        throw new BusinessException(responseStatus, "DOCUMENT_NOT_EXTRACTABLE",
                                "The document could not be read as a text-based PDF or DOCX");
                    })
                    .body(JsonNode.class);
            String text = result == null ? "" : result.path("text").asText("");
            if (text.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_INTELLIGENCE_RESPONSE",
                        "Contract intelligence returned no document text");
            }
            return text;
        } catch (RestClientException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "CONTRACT_INTELLIGENCE_FAILED",
                    "Contract intelligence could not extract the document", exception);
        }
    }

    public JsonNode analyze(UUID contractId, JsonNode reviewResult, UUID organizationId, UUID actorId) {
        requireConfigured();
        JsonNode clauses = reviewResult.path("clauses");
        try {
            JsonNode result = restClient.post()
                    .uri("/internal/v1/contracts/{contractId}/analysis", contractId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + serviceToken)
                    .header("X-ProcuraX-Organization-Id", organizationId.toString())
                    .header("X-ProcuraX-Actor-Id", actorId.toString())
                    .body(Map.of("clauses", clauses))
                    .retrieve()
                    .body(JsonNode.class);
            if (result == null
                    || !contractId.toString().equals(result.path("contract_id").asText())
                    || !organizationId.toString().equals(result.path("organization_id").asText())
                    || !actorId.toString().equals(result.path("requested_by_user_id").asText())
                    || !result.path("advisory_only").asBoolean(false)
                    || !result.path("requires_human_review").asBoolean(false)
                    || result.path("model").asText("").isBlank()
                    || !citationsAreGrounded(clauses, result.path("summaries"))) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_INTELLIGENCE_RESPONSE",
                        "Contract intelligence returned an invalid analysis response");
            }
            return result;
        } catch (RestClientException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "CONTRACT_INTELLIGENCE_FAILED",
                    "Contract intelligence could not analyze the evidence", exception);
        }
    }

    public ContractEmbeddingResponse embed(UUID contractId, String contractText,
                                           UUID organizationId, UUID actorId) {
        requireConfigured();
        try {
            ContractEmbeddingResponse result = restClient.post()
                    .uri("/internal/v1/contracts/{contractId}/embeddings", contractId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + serviceToken)
                    .header("X-ProcuraX-Organization-Id", organizationId.toString())
                    .header("X-ProcuraX-Actor-Id", actorId.toString())
                    .body(Map.of("contract_text", contractText))
                    .retrieve()
                    .body(ContractEmbeddingResponse.class);
            if (result == null
                    || !contractId.equals(result.contractId())
                    || !organizationId.equals(result.organizationId())
                    || !actorId.equals(result.requestedByUserId())
                    || !embeddingModel.equals(result.model())
                    || result.dimensions() != embeddingDimensions
                    || result.chunks() == null
                    || result.chunks().isEmpty()
                    || result.chunks().size() > 100) {
                throw invalidEmbeddingResponse();
            }
            for (int i = 0; i < result.chunks().size(); i++) {
                ContractEmbeddingResponse.Chunk chunk = result.chunks().get(i);
                if (chunk.chunkId() != i + 1
                        || chunk.startCharacter() < 0
                        || chunk.endCharacter() <= chunk.startCharacter()
                        || chunk.endCharacter() > contractText.length()
                        || chunk.embedding() == null
                        || chunk.embedding().size() != embeddingDimensions
                        || !contractText.substring(chunk.startCharacter(), chunk.endCharacter()).equals(chunk.text())
                        || chunk.embedding().stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
                    throw invalidEmbeddingResponse();
                }
            }
            return result;
        } catch (RestClientException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "CONTRACT_EMBEDDING_FAILED",
                    "Contract intelligence could not index the document", exception);
        }
    }

    private BusinessException invalidEmbeddingResponse() {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_EMBEDDING_RESPONSE",
                "Contract intelligence returned an invalid embedding response");
    }

    private static boolean citationsAreGrounded(JsonNode clauses, JsonNode summaries) {
        if (!summaries.isArray() || summaries.size() != clauses.size()) {
            return false;
        }
        for (int i = 0; i < clauses.size(); i++) {
            Set<Integer> allowed = new HashSet<>();
            clauses.get(i).path("citations").forEach(c -> allowed.add(c.path("chunk_id").asInt()));
            JsonNode summary = summaries.get(i);
            if (!clauses.get(i).path("clause").asText().equals(summary.path("clause").asText())) {
                return false;
            }
            for (JsonNode cited : summary.path("cited_chunk_ids")) {
                if (!allowed.contains(cited.asInt())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requireConfigured() {
        if (serviceToken.length() < 32) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CONTRACT_INTELLIGENCE_UNAVAILABLE",
                    "Contract intelligence is not configured");
        }
    }
}
