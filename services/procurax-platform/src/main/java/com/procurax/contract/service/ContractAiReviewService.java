package com.procurax.contract.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.error.BusinessException;
import com.procurax.contract.domain.Contract;
import com.procurax.contract.repository.ContractAiAnalysisRepository;
import com.procurax.contract.domain.ContractAiReview;
import com.procurax.contract.web.ContractAiAnalysisResponse;
import com.procurax.contract.repository.ContractAiReviewRepository;
import com.procurax.contract.repository.ContractRepository;
import com.procurax.contract.web.ContractAiReviewResponse;
import com.procurax.contract.web.ContractSemanticSearchRequest;
import com.procurax.contract.web.ContractSemanticSearchResponse;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.vendor.repository.VendorMembershipRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContractAiReviewService {

    private final ContractRepository contractRepository;
    private final ContractAiReviewRepository reviewRepository;
    private final ContractAiAnalysisRepository analysisRepository;
    private final JdbcTemplate jdbc;
    private final VendorMembershipRepository vendorMembershipRepository;
    private final ContractIntelligenceClient intelligenceClient;
    private final ContractAiReviewRecorder reviewRecorder;
    private final OrganizationContext organizationContext;

    public ContractAiReviewService(ContractRepository contractRepository,
                                   ContractAiReviewRepository reviewRepository,
                                   ContractAiAnalysisRepository analysisRepository,
                                   JdbcTemplate jdbc,
                                   VendorMembershipRepository vendorMembershipRepository,
                                   ContractIntelligenceClient intelligenceClient,
                                   ContractAiReviewRecorder reviewRecorder,
                                   OrganizationContext organizationContext) {
        this.contractRepository = contractRepository;
        this.reviewRepository = reviewRepository;
        this.analysisRepository = analysisRepository;
        this.jdbc = jdbc;
        this.vendorMembershipRepository = vendorMembershipRepository;
        this.intelligenceClient = intelligenceClient;
        this.reviewRecorder = reviewRecorder;
        this.organizationContext = organizationContext;
    }

    public ContractAiReviewResponse review(UUID contractId) {
        return review(contractId, null);
    }

    public ContractAiReviewResponse review(UUID contractId, MultipartFile document) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = draftContract(contractId, organizationId, principal);
        String text = contract.getContent();
        if (document != null) {
            if (document.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "EMPTY_DOCUMENT", "Document is empty");
            }
            if (document.getSize() > 10 * 1024 * 1024) {
                throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "DOCUMENT_TOO_LARGE",
                        "Document exceeds the 10 MB limit");
            }
            try {
                text = intelligenceClient.extractText(document.getBytes(), organizationId, principal.getUserId());
            } catch (IOException exception) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "DOCUMENT_UNREADABLE",
                        "Document could not be read", exception);
            }
        }
        JsonNode result = intelligenceClient.review(contractId, text, organizationId, principal.getUserId());
        var embeddings = intelligenceClient.embed(contractId, text, organizationId, principal.getUserId());
        return ContractAiReviewResponse.from(reviewRecorder.record(
                contract, principal.getUserId(), result, embeddings));
    }

    public ContractAiAnalysisResponse analyze(UUID contractId, UUID reviewId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = draftContract(contractId, organizationId, principal);
        ContractAiReview review = reviewRepository.findByIdAndOrganizationIdAndContractId(
                        reviewId, organizationId, contractId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CONTRACT_AI_REVIEW_NOT_FOUND",
                        "AI review not found"));
        JsonNode result = intelligenceClient.analyze(contractId, review.getReviewResult(),
                organizationId, principal.getUserId());
        return ContractAiAnalysisResponse.from(reviewRecorder.recordAnalysis(
                contract, review, principal.getUserId(), result));
    }

    public List<ContractAiAnalysisResponse> listAnalyses(UUID contractId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract, principal);
        return analysisRepository.findAllByOrganizationIdAndContractIdOrderByCreatedAtDesc(
                        organizationId, contractId)
                .stream().map(ContractAiAnalysisResponse::from).toList();
    }

    public ContractSemanticSearchResponse semanticSearch(UUID contractId, ContractSemanticSearchRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract, principal);
        var queryEmbedding = intelligenceClient.embed(
                contractId, request.query(), organizationId, principal.getUserId());
        var vector = queryEmbedding.chunks().getFirst().embedding();
        String vectorLiteral = ContractVector.toLiteral(vector);
        List<ContractSemanticSearchResponse.Match> matches = jdbc.query("""
                SELECT review_id, chunk_index, start_character, end_character, content,
                       1 - (embedding <=> ?::vector) AS cosine_similarity
                FROM contract_ai_review_chunks
                WHERE organization_id = ? AND contract_id = ?
                  AND review_id = (
                      SELECT id FROM contract_ai_reviews
                      WHERE organization_id = ? AND contract_id = ?
                      ORDER BY created_at DESC, id DESC
                      LIMIT 1
                  )
                ORDER BY embedding <=> ?::vector
                LIMIT 5
                """, (row, index) -> new ContractSemanticSearchResponse.Match(
                        row.getObject("review_id", UUID.class),
                        row.getInt("chunk_index"),
                        row.getInt("start_character"),
                        row.getInt("end_character"),
                        row.getString("content"),
                        row.getDouble("cosine_similarity")),
                vectorLiteral, organizationId, contractId, organizationId, contractId, vectorLiteral);
        return new ContractSemanticSearchResponse(contractId, queryEmbedding.model(), matches);
    }

    private Contract draftContract(UUID contractId, UUID organizationId, SecurityPrincipal principal) {
        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract, principal);
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRACT_NOT_REVIEWABLE",
                    "Only draft contracts can receive AI evidence reviews");
        }
        return contract;
    }

    public List<ContractAiReviewResponse> list(UUID contractId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract, principal);
        return reviewRepository.findAllByOrganizationIdAndContractIdOrderByCreatedAtDesc(
                        organizationId, contractId)
                .stream().map(ContractAiReviewResponse::from).toList();
    }

    private void requireVisibleToCurrentUser(Contract contract, SecurityPrincipal principal) {
        if ("VENDOR".equals(principal.getRole())
                && !vendorMembershipRepository.existsByOrganizationIdAndVendorIdAndUserId(
                        contract.getOrganizationId(), contract.getVendorId(), principal.getUserId())) {
            throw notFound();
        }
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "CONTRACT_NOT_FOUND", "Contract not found");
    }
}
