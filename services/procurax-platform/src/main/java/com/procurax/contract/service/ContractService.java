package com.procurax.contract.service;

import com.procurax.common.error.BusinessException;
import com.procurax.contract.domain.Contract;
import com.procurax.contract.domain.ContractAudit;
import com.procurax.contract.domain.ContractDecision;
import com.procurax.contract.repository.ContractAuditRepository;
import com.procurax.contract.repository.ContractDecisionRepository;
import com.procurax.contract.repository.ContractRepository;
import com.procurax.contract.web.ContractAuditRequest;
import com.procurax.contract.web.ContractAuditResponse;
import com.procurax.contract.web.ContractDecisionRequest;
import com.procurax.contract.web.ContractDecisionResponse;
import com.procurax.contract.web.ContractDocumentResponse;
import com.procurax.contract.web.ContractDocumentDownloadResponse;
import com.procurax.contract.web.ContractResponse;
import com.procurax.contract.web.CreateContractRequest;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.quotation.domain.Quotation;
import com.procurax.quotation.repository.QuotationRepository;
import com.procurax.vendor.domain.Vendor;
import com.procurax.vendor.domain.VendorMembership;
import com.procurax.vendor.repository.VendorMembershipRepository;
import com.procurax.vendor.repository.VendorRepository;
import com.procurax.vendor.service.DocumentStorage;
import com.procurax.vendor.service.StoredDocument;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractAuditRepository contractAuditRepository;
    private final ContractDecisionRepository contractDecisionRepository;
    private final QuotationRepository quotationRepository;
    private final VendorRepository vendorRepository;
    private final VendorMembershipRepository vendorMembershipRepository;
    private final DocumentStorage documentStorage;
    private final OrganizationContext organizationContext;
    private final OutboxEventWriter eventWriter;

    public ContractService(ContractRepository contractRepository, ContractAuditRepository contractAuditRepository,
                           ContractDecisionRepository contractDecisionRepository,
                           QuotationRepository quotationRepository, VendorRepository vendorRepository,
                           VendorMembershipRepository vendorMembershipRepository,
                           DocumentStorage documentStorage, OrganizationContext organizationContext,
                           OutboxEventWriter eventWriter) {
        this.contractRepository = contractRepository;
        this.contractAuditRepository = contractAuditRepository;
        this.contractDecisionRepository = contractDecisionRepository;
        this.quotationRepository = quotationRepository;
        this.vendorRepository = vendorRepository;
        this.vendorMembershipRepository = vendorMembershipRepository;
        this.documentStorage = documentStorage;
        this.organizationContext = organizationContext;
        this.eventWriter = eventWriter;
    }

    @Transactional
    public ContractResponse create(CreateContractRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Quotation quotation = quotationRepository.findForPurchaseOrder(request.quotationId(), organizationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "QUOTATION_NOT_FOUND", "Quotation not found"));
        if (!"ACCEPTED".equals(quotation.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUOTATION_NOT_ACCEPTED",
                    "A contract can only be drafted from an accepted quotation");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONTRACT_DATES",
                    "Contract end date must be on or after its start date");
        }
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(quotation.getVendorId(), organizationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND", "Vendor not found"));
        Contract contract = new Contract(organizationId, quotation.getRfqId(), quotation.getId(),
                vendor.getId(), request.content().trim(), request.startDate(), request.endDate());
        contract.setCreatedBy(principal.getUserId());
        Contract saved = contractRepository.saveAndFlush(contract);
        eventWriter.record("CONTRACT", saved.getId(), organizationId, "CONTRACT_DRAFT_CREATED",
                "procurax.contract.v1", OutboxEventWriter.currentCorrelationId(saved.getId()),
                principal.getUserId(), Map.of("quotationId", saved.getQuotationId(),
                        "rfqId", saved.getRfqId(), "vendorId", saved.getVendorId(),
                        "status", saved.getStatus()));
        return ContractResponse.from(saved, vendor.getName());
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> list(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        var pageable = PageRequest.of(page, size);
        var contracts = "VENDOR".equals(principal.getRole())
                ? contractRepository.findAllByOrganizationIdAndVendorIdInOrderByCreatedAtDesc(
                        organizationId, vendorIdsForUser(organizationId, principal.getUserId()), pageable)
                : contractRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
        return contracts
                .getContent().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ContractResponse get(UUID id) {
        UUID organizationId = organizationContext.currentOrganizationId();
        Contract contract = contractRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract);
        return response(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractAuditResponse> listAudits(UUID contractId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract);
        return contractAuditRepository.findAllByOrganizationIdAndContractIdOrderByCreatedAtAsc(
                        organizationId, contractId)
                .stream().map(ContractAuditResponse::from).toList();
    }

    @Transactional
    public ContractAuditResponse addAudit(UUID contractId, ContractAuditRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = contractRepository.findForUpdate(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract);
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRACT_NOT_AUDITABLE",
                    "Only a draft contract can receive review findings");
        }

        ContractAudit audit = new ContractAudit(organizationId, contractId,
                request.riskLevel().name(), request.finding().trim(), trimToNull(request.clause()),
                trimToNull(request.explanation()), trimToNull(request.recommendation()), request.confidence());
        audit.setCreatedBy(principal.getUserId());
        ContractAudit savedAudit = contractAuditRepository.saveAndFlush(audit);
        contract.markAuditCompleted();
        contract.setUpdatedBy(principal.getUserId());
        contractRepository.saveAndFlush(contract);

        Map<String, Object> payload = Map.of(
                "contractAuditId", savedAudit.getId(),
                "contractId", contractId,
                "riskLevel", savedAudit.getRiskLevel(),
                "auditStatus", contract.getAuditStatus());
        eventWriter.record("CONTRACT", contractId, organizationId, "CONTRACT_AUDIT_RECORDED",
                "procurax.contract.v1", OutboxEventWriter.currentCorrelationId(contractId),
                principal.getUserId(), payload);
        return ContractAuditResponse.from(savedAudit);
    }

    @Transactional(readOnly = true)
    public List<ContractDecisionResponse> listDecisions(UUID contractId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .map(contract -> {
                    requireVisibleToCurrentUser(contract);
                    return contract;
                }).orElseThrow(this::notFound);
        return contractDecisionRepository.findAllByOrganizationIdAndContractIdOrderByDecidedAtDesc(
                        organizationId, contractId)
                .stream().map(ContractDecisionResponse::from).toList();
    }

    @Transactional
    public ContractDecisionResponse decide(UUID contractId, ContractDecisionRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = contractRepository.findForUpdate(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract);
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRACT_ALREADY_DECIDED",
                    "Only a draft contract can be approved or rejected");
        }
        if (principal.getUserId().equals(contract.getCreatedBy())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "CONTRACT_SELF_APPROVAL",
                    "The contract creator cannot decide their own contract");
        }
        String comment = request.comment() == null ? "" : request.comment().trim();
        if ("REJECT".equals(request.decision().name()) && comment.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CONTRACT_REJECTION_COMMENT_REQUIRED",
                    "A rejection comment is required");
        }

        String persistedDecision = "APPROVE".equals(request.decision().name()) ? "APPROVED" : "REJECTED";
        ContractDecision decision = new ContractDecision(organizationId, contractId,
                persistedDecision, comment);
        decision.setCreatedBy(principal.getUserId());
        ContractDecision saved = contractDecisionRepository.saveAndFlush(decision);
        contract.decide(persistedDecision);
        contract.setUpdatedBy(principal.getUserId());
        contractRepository.saveAndFlush(contract);
        eventWriter.record("CONTRACT", contractId, organizationId,
                "APPROVED".equals(persistedDecision) ? "CONTRACT_APPROVED" : "CONTRACT_REJECTED",
                "procurax.contract.v1", OutboxEventWriter.currentCorrelationId(contractId),
                principal.getUserId(), Map.of("contractDecisionId", saved.getId(),
                        "decision", persistedDecision, "status", contract.getStatus()));
        return ContractDecisionResponse.from(saved);
    }

    @Transactional
    public ContractDocumentResponse uploadDocument(UUID contractId, MultipartFile file) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Contract contract = contractRepository.findForUpdate(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract);
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRACT_DOCUMENT_LOCKED",
                    "Documents can only be attached to draft contracts");
        }
        if (contract.getFileUrl() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "CONTRACT_DOCUMENT_ALREADY_ATTACHED",
                    "A contract document is already attached; create a new draft to replace it");
        }
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONTRACT_DOCUMENT_SIZE",
                    "A non-empty contract document no larger than 10 MB is required");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank() || fileName.length() > 255
                || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains("\r") || fileName.contains("\n")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONTRACT_DOCUMENT_NAME",
                    "The contract document must have a valid file name");
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String expectedContentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CONTRACT_DOCUMENT",
                    "Contract documents must be PDF or DOCX files");
        };
        if (!expectedContentType.equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CONTRACT_DOCUMENT_CONTENT_TYPE_MISMATCH",
                    "Document extension and content type do not match");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CONTRACT_DOCUMENT_READ_FAILED",
                    "The contract document could not be read", exception);
        }
        if (!validDocumentSignature(extension, content)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CONTRACT_DOCUMENT_CONTENT",
                    "The contract document content does not match its file type");
        }
        StoredDocument stored;
        try {
            stored = documentStorage.upload(content, fileName, "raw",
                    "procurement/contract_documents", "authenticated");
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "CONTRACT_DOCUMENT_STORAGE_FAILED",
                    "The contract document could not be stored", exception);
        }
        try {
            String url = validateCloudinaryUrl(stored.secureUrl());
            if (!"raw".equals(stored.resourceType()) || !"authenticated".equals(stored.deliveryType())
                    || stored.publicId().isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_STORAGE_RESPONSE",
                        "Cloudinary returned invalid contract document metadata");
            }
            contract.attachDocument(url, stored.publicId(), stored.resourceType(),
                    stored.deliveryType(), fileName);
            contract.setUpdatedBy(principal.getUserId());
            Contract saved = contractRepository.saveAndFlush(contract);
            eventWriter.record("CONTRACT", contractId, organizationId, "CONTRACT_DOCUMENT_UPLOADED",
                    "procurax.contract.v1", OutboxEventWriter.currentCorrelationId(contractId),
                    principal.getUserId(), Map.of("contractId", contractId, "fileName", fileName));
            return ContractDocumentResponse.from(saved);
        } catch (RuntimeException exception) {
            try {
                documentStorage.delete(stored.publicId(), stored.resourceType(), stored.deliveryType());
            } catch (IOException | RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ContractDocumentDownloadResponse createDocumentDownload(UUID contractId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        Contract contract = contractRepository.findByIdAndOrganizationId(contractId, organizationId)
                .orElseThrow(this::notFound);
        requireVisibleToCurrentUser(contract);
        if (contract.getCloudinaryPublicId() == null
                || !"raw".equals(contract.getCloudinaryResourceType())
                || !"authenticated".equals(contract.getCloudinaryDeliveryType())
                || contract.getDocumentFileName() == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "CONTRACT_DOCUMENT_NOT_FOUND",
                    "Contract document not found");
        }
        String fileName = contract.getDocumentFileName();
        String format = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        Instant expiresAt = Instant.now().plusSeconds(300);
        try {
            String downloadUrl = documentStorage.createDownloadUrl(contract.getCloudinaryPublicId(),
                    format, contract.getCloudinaryResourceType(), contract.getCloudinaryDeliveryType(),
                    fileName, expiresAt);
            return new ContractDocumentDownloadResponse(validateDownloadUrl(downloadUrl), expiresAt);
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "CONTRACT_DOCUMENT_DOWNLOAD_FAILED",
                    "A secure contract document link could not be created", exception);
        }
    }

    private String validateDownloadUrl(String value) {
        if (value == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_DOWNLOAD_URL",
                    "Cloudinary returned an invalid signed download URL");
        }
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"api.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null || uri.getPort() != -1 || uri.getFragment() != null
                    || uri.getPath() == null || !uri.getPath().matches("/v1_1/[A-Za-z0-9_-]+/raw/download")
                    || uri.getQuery() == null || !uri.getQuery().contains("signature=")
                    || !uri.getQuery().contains("expires_at=")) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_DOWNLOAD_URL",
                    "Cloudinary returned an invalid signed download URL");
        }
    }

    private boolean validDocumentSignature(String extension, byte[] content) {
        if (content.length < 4) {
            return false;
        }
        if ("pdf".equals(extension)) {
            return content.length >= 5 && content[0] == '%' && content[1] == 'P'
                    && content[2] == 'D' && content[3] == 'F' && content[4] == '-';
        }
        return content[0] == 'P' && content[1] == 'K' && content[2] == 0x03 && content[3] == 0x04;
    }

    private String validateCloudinaryUrl(String value) {
        if (value == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_STORAGE_RESPONSE",
                    "Cloudinary returned an invalid document URL");
        }
        try {
            java.net.URI uri = new java.net.URI(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null || uri.getPort() != -1 || uri.getFragment() != null
                    || uri.getPath() == null || !uri.getPath().matches("/[A-Za-z0-9_-]+/.*")) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (java.net.URISyntaxException | IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "INVALID_CONTRACT_STORAGE_RESPONSE",
                    "Cloudinary returned an invalid document URL");
        }
    }

    private ContractResponse response(Contract contract) {
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(
                        contract.getVendorId(), contract.getOrganizationId())
                .orElseThrow(this::notFound);
        return ContractResponse.from(contract, vendor.getName());
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "CONTRACT_NOT_FOUND", "Contract not found");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requireVisibleToCurrentUser(Contract contract) {
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        if ("VENDOR".equals(principal.getRole())
                && !vendorMembershipRepository.existsByOrganizationIdAndVendorIdAndUserId(
                        contract.getOrganizationId(), contract.getVendorId(), principal.getUserId())) {
            throw notFound();
        }
    }

    private List<UUID> vendorIdsForUser(UUID organizationId, UUID userId) {
        return vendorMembershipRepository.findAllByOrganizationIdAndUserId(organizationId, userId)
                .stream().map(VendorMembership::getVendorId).toList();
    }
}
