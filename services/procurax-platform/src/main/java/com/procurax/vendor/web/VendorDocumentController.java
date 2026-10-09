package com.procurax.vendor.web;

import com.procurax.vendor.service.VendorDocumentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/documents")
public class VendorDocumentController {

    private final VendorDocumentService documentService;

    public VendorDocumentController(VendorDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public List<VendorDocumentResponse> list(@PathVariable UUID vendorId) {
        return documentService.list(vendorId);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('VENDOR_DOCUMENT_SUBMIT', 'VENDOR_UPDATE')")
    public VendorDocumentResponse create(@PathVariable UUID vendorId,
                                         @Valid @RequestBody CreateVendorDocumentRequest request) {
        return documentService.create(vendorId, request);
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('VENDOR_DOCUMENT_SUBMIT', 'VENDOR_UPDATE')")
    public VendorDocumentResponse upload(@PathVariable UUID vendorId,
                                         @RequestParam("documentType") String documentType,
                                         @RequestPart("file") MultipartFile file) {
        return documentService.upload(vendorId, documentType, file);
    }

    @PatchMapping("/{documentId}/verification")
    @PreAuthorize("hasAuthority('VENDOR_DOCUMENT_VERIFY')")
    public VendorDocumentResponse verify(@PathVariable UUID vendorId, @PathVariable UUID documentId,
                                         @Valid @RequestBody VerifyVendorDocumentRequest request) {
        return documentService.verify(vendorId, documentId, request);
    }
}
