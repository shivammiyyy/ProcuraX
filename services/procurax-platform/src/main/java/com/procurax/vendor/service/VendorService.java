package com.procurax.vendor.service;

import com.procurax.common.error.BusinessException;
import com.procurax.identity.repository.OrganizationMemberRepository;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.vendor.domain.Vendor;
import com.procurax.vendor.domain.VendorMembership;
import com.procurax.vendor.repository.VendorMembershipRepository;
import com.procurax.vendor.repository.VendorRepository;
import com.procurax.vendor.web.CreateVendorRequest;
import com.procurax.vendor.web.UpdateVendorRequest;
import com.procurax.vendor.web.VendorSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMembershipRepository vendorMembershipRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationContext organizationContext;
    private final OutboxEventWriter eventWriter;

    public VendorService(VendorRepository vendorRepository,
                         VendorMembershipRepository vendorMembershipRepository,
                         OrganizationMemberRepository organizationMemberRepository,
                         OrganizationContext organizationContext, OutboxEventWriter eventWriter) {
        this.vendorRepository = vendorRepository;
        this.vendorMembershipRepository = vendorMembershipRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.organizationContext = organizationContext;
        this.eventWriter = eventWriter;
    }

    @Transactional(readOnly = true)
    public List<VendorSummary> list(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        return vendorRepository.findAllByOrganizationId(organizationId, PageRequest.of(page, size))
                .map(VendorSummary::from).getContent();
    }

    @Transactional(readOnly = true)
    public VendorSummary get(UUID vendorId) {
        return VendorSummary.from(requireVendor(vendorId));
    }

    @Transactional
    public VendorSummary create(CreateVendorRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        if (vendorRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, request.name().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "VENDOR_ALREADY_EXISTS",
                    "A vendor with this name already exists in the organization");
        }
        Vendor vendor = new Vendor(organizationId, request.name().trim(), request.contactEmail(),
                request.category(), request.paymentTermsDays());
        SecurityPrincipal actor = organizationContext.currentPrincipal();
        vendor.setCreatedBy(actor.getUserId());
        vendor.setUpdatedBy(actor.getUserId());
        Vendor saved = vendorRepository.save(vendor);
        eventWriter.record("VENDOR", saved.getId(), organizationId, "VENDOR_CREATED",
                "procurax.vendor.v1", OutboxEventWriter.currentCorrelationId(UUID.randomUUID()),
                actor.getUserId(), Map.of("name", saved.getName(),
                        "category", java.util.Objects.toString(saved.getCategory(), "")));
        return VendorSummary.from(saved);
    }

    @Transactional
    public VendorSummary update(UUID vendorId, UpdateVendorRequest request) {
        Vendor vendor = requireVendor(vendorId);
        if (!vendor.getName().equalsIgnoreCase(request.name().trim())
                && vendorRepository.existsByOrganizationIdAndNameIgnoreCase(
                        vendor.getOrganizationId(), request.name().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "VENDOR_ALREADY_EXISTS",
                    "A vendor with this name already exists in the organization");
        }
        SecurityPrincipal actor = organizationContext.currentPrincipal();
        vendor.update(request.name().trim(), request.contactEmail(), request.category(), request.paymentTermsDays());
        vendor.setUpdatedBy(actor.getUserId());
        Vendor saved = vendorRepository.save(vendor);
        eventWriter.record("VENDOR", saved.getId(), saved.getOrganizationId(), "VENDOR_UPDATED",
                "procurax.vendor.v1", OutboxEventWriter.currentCorrelationId(UUID.randomUUID()),
                actor.getUserId(), Map.of("name", saved.getName(),
                        "category", java.util.Objects.toString(saved.getCategory(), "")));
        return VendorSummary.from(saved);
    }

    @Transactional
    public void addMember(UUID vendorId, UUID userId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        Vendor vendor = requireVendor(vendorId);
        if (!organizationMemberRepository.existsByOrganizationIdAndUserIdAndStatus(
                organizationId, userId, "ACTIVE")) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ORGANIZATION_MEMBER_NOT_FOUND",
                    "User is not an active member of this organization");
        }
        if (vendorMembershipRepository.existsByOrganizationIdAndVendorIdAndUserId(
                organizationId, vendorId, userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "VENDOR_MEMBER_ALREADY_EXISTS",
                    "This user is already associated with the vendor");
        }
        SecurityPrincipal actor = organizationContext.currentPrincipal();
        vendorMembershipRepository.save(new VendorMembership(organizationId, vendor.getId(), userId, actor.getUserId()));
        eventWriter.record("VENDOR", vendor.getId(), organizationId, "VENDOR_MEMBER_ADDED",
                "procurax.vendor.v1", OutboxEventWriter.currentCorrelationId(UUID.randomUUID()),
                actor.getUserId(), Map.of("vendorId", vendor.getId(), "userId", userId));
    }

    @Transactional(readOnly = true)
    public Vendor requireVendor(UUID vendorId) {
        return vendorRepository.findByIdAndOrganizationId(vendorId, organizationContext.currentOrganizationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND",
                        "Vendor not found"));
    }
}
