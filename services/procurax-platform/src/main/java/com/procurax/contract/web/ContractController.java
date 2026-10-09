package com.procurax.contract.web;

import com.procurax.contract.service.ContractService;
import com.procurax.contract.service.ContractAiReviewService;
import com.procurax.contract.web.ContractSemanticSearchRequest;
import com.procurax.contract.web.ContractSemanticSearchResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService contractService;
    private final ContractAiReviewService contractAiReviewService;

    public ContractController(ContractService contractService,
                              ContractAiReviewService contractAiReviewService) {
        this.contractService = contractService;
        this.contractAiReviewService = contractAiReviewService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public List<ContractResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return contractService.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public ContractResponse get(@PathVariable UUID id) {
        return contractService.get(id);
    }

    @PostMapping(path = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONTRACT_CREATE')")
    public ContractDocumentResponse uploadDocument(@PathVariable UUID id,
                                                   @RequestPart("file") MultipartFile file) {
        return contractService.uploadDocument(id, file);
    }

    @GetMapping("/{id}/document/download")
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public ResponseEntity<ContractDocumentDownloadResponse> createDocumentDownload(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(contractService.createDocumentDownload(id));
    }

    @GetMapping("/{id}/ai-reviews")
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public List<ContractAiReviewResponse> listAiReviews(@PathVariable UUID id) {
        return contractAiReviewService.list(id);
    }

    @PostMapping("/{id}/ai-reviews")
    @PreAuthorize("hasAuthority('CONTRACT_APPROVE')")
    public ContractAiReviewResponse reviewWithAi(@PathVariable UUID id) {
        return contractAiReviewService.review(id);
    }

    @PostMapping(path = "/{id}/ai-reviews/from-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONTRACT_APPROVE')")
    public ContractAiReviewResponse reviewDocumentWithAi(@PathVariable UUID id,
                                                         @RequestPart("file") MultipartFile file) {
        return contractAiReviewService.review(id, file);
    }

    @GetMapping("/{id}/ai-analyses")
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public List<ContractAiAnalysisResponse> listAiAnalyses(@PathVariable UUID id) {
        return contractAiReviewService.listAnalyses(id);
    }

    @PostMapping("/{id}/semantic-search")
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public ContractSemanticSearchResponse semanticSearch(
            @PathVariable UUID id, @Valid @RequestBody ContractSemanticSearchRequest request) {
        return contractAiReviewService.semanticSearch(id, request);
    }

    @PostMapping("/{id}/ai-reviews/{reviewId}/analysis")
    @PreAuthorize("hasAuthority('CONTRACT_APPROVE')")
    public ContractAiAnalysisResponse analyzeReview(@PathVariable UUID id, @PathVariable UUID reviewId) {
        return contractAiReviewService.analyze(id, reviewId);
    }

    @GetMapping("/{id}/audits")
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public List<ContractAuditResponse> listAudits(@PathVariable UUID id) {
        return contractService.listAudits(id);
    }

    @GetMapping("/{id}/decisions")
    @PreAuthorize("hasAuthority('CONTRACT_READ')")
    public List<ContractDecisionResponse> listDecisions(@PathVariable UUID id) {
        return contractService.listDecisions(id);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('CONTRACT_APPROVE')")
    public ContractDecisionResponse decide(@PathVariable UUID id,
                                           @Valid @RequestBody ContractDecisionRequest request) {
        return contractService.decide(id, request);
    }

    @PostMapping("/{id}/audits")
    @PreAuthorize("hasAuthority('CONTRACT_APPROVE')")
    public ContractAuditResponse addAudit(@PathVariable UUID id,
                                          @Valid @RequestBody ContractAuditRequest request) {
        return contractService.addAudit(id, request);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRACT_CREATE')")
    public ContractResponse create(@Valid @RequestBody CreateContractRequest request) {
        return contractService.create(request);
    }
}
