package com.procurax.vendor.service;

import com.procurax.common.error.BusinessException;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.vendor.domain.VendorDocument;
import com.procurax.vendor.domain.VendorDocumentStatus;
import com.procurax.vendor.repository.VendorDocumentRepository;
import com.procurax.vendor.repository.VendorMembershipRepository;
import com.procurax.vendor.web.CreateVendorDocumentRequest;
import com.procurax.vendor.web.VendorDocumentResponse;
import com.procurax.vendor.web.VerifyVendorDocumentRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VendorDocumentService {

    private static final Logger log = LoggerFactory.getLogger(VendorDocumentService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final VendorService vendorService;
    private final VendorDocumentRepository documentRepository;
    private final VendorMembershipRepository membershipRepository;
    private final OrganizationContext organizationContext;
    private final DocumentStorage documentStorage;
    private final OutboxEventWriter eventWriter;

    public VendorDocumentService(VendorService vendorService, VendorDocumentRepository documentRepository,
                                 VendorMembershipRepository membershipRepository,
                                 OrganizationContext organizationContext, DocumentStorage documentStorage,
                                 OutboxEventWriter eventWriter) {
        this.vendorService = vendorService;
        this.documentRepository = documentRepository;
        this.membershipRepository = membershipRepository;
        this.organizationContext = organizationContext;
        this.documentStorage = documentStorage;
        this.eventWriter = eventWriter;
    }

    @Transactional(readOnly = true)
    public List<VendorDocumentResponse> list(UUID vendorId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        vendorService.requireVendor(vendorId);
        requireVendorMembershipIfVendor(vendorId);
        return documentRepository.findAllByOrganizationIdAndVendorIdOrderByCreatedAtDesc(
                        organizationId, vendorId)
                .stream().map(VendorDocumentResponse::from).toList();
    }

    @Transactional
    public VendorDocumentResponse create(UUID vendorId, CreateVendorDocumentRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        vendorService.requireVendor(vendorId);
        SecurityPrincipal actor = organizationContext.currentPrincipal();
        if ("VENDOR".equals(actor.getRole())) {
            requireVendorMembership(vendorId, actor.getUserId());
        } else if (!actor.getPermissions().contains("VENDOR_UPDATE")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "VENDOR_DOCUMENT_SUBMIT_FORBIDDEN",
                    "Only an associated vendor or authorized procurement user can submit documents");
        }

        String storageUrl = validateCloudinaryUrl(request.storageUrl());
        String fileName = request.fileName().trim();
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("\r")
                || fileName.contains("\n") || !fileName.matches("(?i).+\\.(pdf|docx|jpe?g|png)")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_DOCUMENT_TYPE",
                    "Vendor documents must be PDF, DOCX, JPG, or PNG files");
        }

        VendorDocument document = new VendorDocument(organizationId, vendorId,
                request.documentType(), fileName, storageUrl);
        document.setCreatedBy(actor.getUserId());
        document.setUpdatedBy(actor.getUserId());
        VendorDocument saved = documentRepository.save(document);
        recordDocumentEvent(saved, actor.getUserId(), "VENDOR_DOCUMENT_REGISTERED");
        return VendorDocumentResponse.from(saved);
    }

    @Transactional
    public VendorDocumentResponse upload(UUID vendorId, String documentType, MultipartFile file) {
        UUID organizationId = organizationContext.currentOrganizationId();
        vendorService.requireVendor(vendorId);
        SecurityPrincipal actor = organizationContext.currentPrincipal();
        if ("VENDOR".equals(actor.getRole())) {
            requireVendorMembership(vendorId, actor.getUserId());
        } else if (!actor.getPermissions().contains("VENDOR_UPDATE")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "VENDOR_DOCUMENT_SUBMIT_FORBIDDEN",
                    "Only an associated vendor or authorized procurement user can submit documents");
        }

        if (documentType == null || !documentType.matches("[A-Z][A-Z0-9_]{1,49}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_TYPE",
                    "Document type must use uppercase letters, digits, or underscores");
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_SIZE",
                    "A non-empty document no larger than 10 MB is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains("\r") || fileName.contains("\n")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_FILE_NAME",
                    "The document must have a valid file name");
        }
        String extension = fileExtension(fileName);
        String expectedContentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_DOCUMENT_TYPE",
                    "Vendor documents must be PDF, DOCX, JPG, or PNG files");
        };
        if (!expectedContentType.equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DOCUMENT_CONTENT_TYPE_MISMATCH",
                    "Document extension and content type do not match");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "DOCUMENT_READ_FAILED",
                    "The uploaded document could not be read", exception);
        }
        validateFileSignature(extension, content);
        String resourceType = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png")
                ? "image" : "raw";
        StoredDocument stored;
        try {
            stored = documentStorage.upload(content, fileName, resourceType,
                    "procurement/vendor_documents", "upload");
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "DOCUMENT_STORAGE_FAILED",
                    "The document could not be stored", exception);
        }
        try {
            String storageUrl = validateCloudinaryUrl(stored.secureUrl(), HttpStatus.BAD_GATEWAY,
                    "INVALID_DOCUMENT_STORAGE_RESPONSE", "Cloudinary returned an invalid document URL");
            VendorDocument document = new VendorDocument(organizationId, vendorId,
                    documentType, fileName, storageUrl);
            document.setCloudinaryAsset(stored.publicId(), stored.resourceType());
            document.setCreatedBy(actor.getUserId());
            document.setUpdatedBy(actor.getUserId());
            VendorDocument saved = documentRepository.saveAndFlush(document);
            recordDocumentEvent(saved, actor.getUserId(), "VENDOR_DOCUMENT_UPLOADED");
            return VendorDocumentResponse.from(saved);
        } catch (RuntimeException exception) {
            try {
                documentStorage.delete(stored.publicId(), stored.resourceType(), stored.deliveryType());
            } catch (IOException | RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
                log.error("Failed to clean up Cloudinary document {} after database persistence failed",
                        stored.publicId(), cleanupException);
            }
            throw exception;
        }
    }

    @Transactional
    public VendorDocumentResponse verify(UUID vendorId, UUID documentId,
                                         VerifyVendorDocumentRequest request) {
        if (request.status() != VendorDocumentStatus.VERIFIED
                && request.status() != VendorDocumentStatus.REJECTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_STATUS",
                    "Document status must be VERIFIED or REJECTED");
        }
        String reason = request.rejectionReason() == null ? null : request.rejectionReason().trim();
        if (request.status() == VendorDocumentStatus.REJECTED
                && (reason == null || reason.isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REJECTION_REASON_REQUIRED",
                    "A reason is required when rejecting a vendor document");
        }
        if (request.status() == VendorDocumentStatus.VERIFIED && reason != null && !reason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "REJECTION_REASON_NOT_ALLOWED",
                    "A rejection reason cannot be supplied when verifying a document");
        }

        UUID organizationId = organizationContext.currentOrganizationId();
        VendorDocument document = documentRepository
                .findByIdAndOrganizationIdAndVendorId(documentId, organizationId, vendorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "VENDOR_DOCUMENT_NOT_FOUND",
                        "Vendor document not found"));
        if (document.getVerificationStatus() != VendorDocumentStatus.UNVERIFIED) {
            throw new BusinessException(HttpStatus.CONFLICT, "VENDOR_DOCUMENT_ALREADY_REVIEWED",
                    "Reviewed documents cannot be changed; submit a new document instead");
        }
        SecurityPrincipal actor = organizationContext.currentPrincipal();
        document.verify(request.status(), actor.getUserId(),
                request.status() == VendorDocumentStatus.REJECTED ? reason : null);
        document.setUpdatedBy(actor.getUserId());
        VendorDocument saved = documentRepository.save(document);
        eventWriter.record("VENDOR_DOCUMENT", saved.getId(), organizationId,
                "VENDOR_DOCUMENT_" + request.status().name(), "procurax.vendor.v1",
                OutboxEventWriter.currentCorrelationId(UUID.randomUUID()), actor.getUserId(),
                java.util.Map.of("vendorId", vendorId, "documentType", saved.getDocumentType(),
                        "verificationStatus", saved.getVerificationStatus().name()));
        return VendorDocumentResponse.from(saved);
    }

    private void recordDocumentEvent(VendorDocument document, UUID actorId, String eventType) {
        eventWriter.record("VENDOR_DOCUMENT", document.getId(), document.getOrganizationId(), eventType,
                "procurax.vendor.v1", OutboxEventWriter.currentCorrelationId(UUID.randomUUID()), actorId,
                java.util.Map.of("vendorId", document.getVendorId(), "documentType", document.getDocumentType(),
                        "verificationStatus", document.getVerificationStatus().name()));
    }

    private void requireVendorMembershipIfVendor(UUID vendorId) {
        SecurityPrincipal actor = organizationContext.currentPrincipal();
        if ("VENDOR".equals(actor.getRole())) {
            requireVendorMembership(vendorId, actor.getUserId());
        }
    }

    private void requireVendorMembership(UUID vendorId, UUID userId) {
        if (!membershipRepository.existsByOrganizationIdAndVendorIdAndUserId(
                organizationContext.currentOrganizationId(), vendorId, userId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND",
                    "Vendor not found");
        }
    }

    private String validateCloudinaryUrl(String value) {
        return validateCloudinaryUrl(value, HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_STORAGE_URL",
                "Document URL must be a secure Cloudinary resource URL");
    }

    private String validateCloudinaryUrl(String value, HttpStatus status, String code, String message) {
        if (value == null) {
            throw new BusinessException(status, code, message);
        }
        try {
            URI uri = new URI(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null || uri.getPort() != -1
                    || uri.getFragment() != null || uri.getPath() == null
                    || !uri.getPath().matches("/[A-Za-z0-9_-]+/.*")) {
                throw new IllegalArgumentException();
            }
            return uri.toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw new BusinessException(status, code, message);
        }
    }

    private String fileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 1 || dotIndex == fileName.length() - 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_DOCUMENT_TYPE",
                    "Vendor documents must be PDF, DOCX, JPG, or PNG files");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void validateFileSignature(String extension, byte[] content) {
        boolean valid = switch (extension) {
            case "pdf" -> startsWith(content, new byte[] {'%', 'P', 'D', 'F', '-'});
            case "png" -> startsWith(content, new byte[] {
                    (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a});
            case "jpg", "jpeg" -> startsWith(content, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
            case "docx" -> startsWith(content, new byte[] {'P', 'K', 0x03, 0x04});
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_CONTENT",
                    "The document content does not match its file type");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
