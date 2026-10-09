package com.procurax.quotation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.correlation.CorrelationIdFilter;
import com.procurax.common.error.BusinessException;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.quotation.domain.Quotation;
import com.procurax.quotation.domain.QuotationItem;
import com.procurax.quotation.domain.VendorScore;
import com.procurax.quotation.repository.QuotationItemRepository;
import com.procurax.quotation.repository.QuotationRepository;
import com.procurax.quotation.repository.VendorScoreRepository;
import com.procurax.quotation.web.CreateQuotationRequest;
import com.procurax.quotation.web.QuotationItemRequest;
import com.procurax.quotation.web.QuotationItemResponse;
import com.procurax.quotation.web.QuotationResponse;
import com.procurax.quotation.web.UpdateQuotationStatusRequest;
import com.procurax.quotation.web.VendorScoreResponse;
import com.procurax.rfq.domain.Rfq;
import com.procurax.rfq.domain.RfqItem;
import com.procurax.rfq.service.RfqService;
import com.procurax.vendor.domain.Vendor;
import com.procurax.vendor.domain.VendorMembership;
import com.procurax.vendor.repository.VendorMembershipRepository;
import com.procurax.vendor.service.VendorService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotationService {

    private static final Set<String> COMPLIANCE_KEYS =
            Set.of("isoCertification", "materialGrade", "environmentalStandards", "documentSubmission");

    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository itemRepository;
    private final VendorScoreRepository scoreRepository;
    private final VendorMembershipRepository vendorMembershipRepository;
    private final OrganizationContext organizationContext;
    private final RfqService rfqService;
    private final VendorService vendorService;
    private final VendorScoringService scoringService;
    private final OutboxEventWriter eventWriter;

    public QuotationService(QuotationRepository quotationRepository,
                           QuotationItemRepository itemRepository,
                           VendorScoreRepository scoreRepository,
                           VendorMembershipRepository vendorMembershipRepository,
                           OrganizationContext organizationContext,
                           RfqService rfqService,
                           VendorService vendorService,
                           VendorScoringService scoringService,
                           OutboxEventWriter eventWriter) {
        this.quotationRepository = quotationRepository;
        this.itemRepository = itemRepository;
        this.scoreRepository = scoreRepository;
        this.vendorMembershipRepository = vendorMembershipRepository;
        this.organizationContext = organizationContext;
        this.rfqService = rfqService;
        this.vendorService = vendorService;
        this.scoringService = scoringService;
        this.eventWriter = eventWriter;
    }

    @Transactional
    public QuotationResponse submit(CreateQuotationRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Rfq rfq = rfqService.requireRfq(request.rfqId());
        validateOpenRfq(rfq);
        if (!rfq.getCurrency().equals(request.currency())) {
            throw badRequest("CURRENCY_MISMATCH", "Quotation currency must match the RFQ currency");
        }
        List<VendorMembership> memberships = vendorMembershipRepository.findAllByOrganizationIdAndUserId(
                organizationId, principal.getUserId());
        UUID vendorId = request.vendorId();
        if (vendorId == null) {
            if (memberships.size() != 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "VENDOR_SELECTION_REQUIRED",
                        "Specify a vendor associated with the authenticated user");
            }
            vendorId = memberships.get(0).getVendorId();
        } else if (memberships.stream().noneMatch(membership -> membership.getVendorId().equals(request.vendorId()))) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "VENDOR_MEMBERSHIP_REQUIRED",
                    "The authenticated user is not associated with the requested vendor");
        }
        Vendor vendor = vendorService.requireVendor(vendorId);
        if (quotationRepository.existsByOrganizationIdAndRfqIdAndVendorId(
                organizationId, rfq.getId(), vendorId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUOTATION_ALREADY_SUBMITTED",
                    "This vendor has already submitted a quotation for the RFQ");
        }
        validateCompliance(request.compliance());
        List<RfqItem> rfqItems = rfqService.items(rfq.getId());
        BigDecimal totalAmount = validateAndCalculateTotal(rfqItems, request.items());
        Quotation quote = new Quotation(organizationId, rfq.getId(), vendorId, totalAmount,
                request.currency(), request.deliveryDays(), request.paymentTermsDays(),
                request.qualityRating(), request.compliance(), correlationId(rfq));
        quote.setCreatedBy(principal.getUserId());
        Quotation saved = quotationRepository.save(quote);
        saveItems(organizationId, saved.getId(), request.items());
        scoreRepository.save(scoringService.score(organizationId, saved, vendor, rfq.getDeliveryDays(), rfq.getBudget()));
        eventWriter.record("QUOTATION", saved.getId(), organizationId, "QUOTATION_SUBMITTED",
                "procurax.quotation.v1", correlationId(rfq), principal.getUserId(),
                Map.of("rfqId", rfq.getId(), "vendorId", vendor.getId(),
                        "totalAmount", saved.getTotalAmount(), "currency", saved.getCurrency(),
                        "deliveryDays", saved.getDeliveryDays()));
        return response(saved);
    }

    @Transactional
    public QuotationResponse update(UUID quotationId, CreateQuotationRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Quotation quotation = requireQuotation(quotationId);
        if (!vendorMembershipRepository.existsByOrganizationIdAndVendorIdAndUserId(
                organizationId, quotation.getVendorId(), principal.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "VENDOR_MEMBERSHIP_REQUIRED",
                    "The authenticated user is not associated with this quotation's vendor");
        }
        if (!quotation.getRfqId().equals(request.rfqId())) {
            throw badRequest("RFQ_MISMATCH", "A quotation cannot be moved to another RFQ");
        }
        if (request.vendorId() != null && !quotation.getVendorId().equals(request.vendorId())) {
            throw badRequest("VENDOR_MISMATCH", "A quotation cannot be moved to another vendor");
        }
        if (!"SUBMITTED".equals(quotation.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUOTATION_NOT_EDITABLE",
                    "Only a submitted quotation can be edited");
        }
        Rfq rfq = rfqService.requireRfq(quotation.getRfqId());
        validateOpenRfq(rfq);
        if (!rfq.getCurrency().equals(request.currency())) {
            throw badRequest("CURRENCY_MISMATCH", "Quotation currency must match the RFQ currency");
        }
        validateCompliance(request.compliance());
        BigDecimal totalAmount = validateAndCalculateTotal(rfqService.items(rfq.getId()), request.items());
        quotation.replace(totalAmount, request.currency(), request.deliveryDays(), request.paymentTermsDays(),
                request.qualityRating(), request.compliance(), correlationId(rfq));
        quotation.setUpdatedBy(principal.getUserId());
        Quotation saved = quotationRepository.save(quotation);
        itemRepository.deleteAll(itemRepository.findAllByOrganizationIdAndQuotationIdOrderByCreatedAt(
                organizationId, quotationId));
        saveItems(organizationId, quotationId, request.items());
        Vendor vendor = vendorService.requireVendor(quotation.getVendorId());
        scoreRepository.deleteAllByOrganizationIdAndQuotationId(organizationId, quotationId);
        scoreRepository.save(scoringService.score(organizationId, saved, vendor, rfq.getDeliveryDays(), rfq.getBudget()));
        eventWriter.record("QUOTATION", saved.getId(), organizationId, "QUOTATION_UPDATED",
                "procurax.quotation.v1", correlationId(rfq), principal.getUserId(),
                Map.of("rfqId", rfq.getId(), "vendorId", saved.getVendorId(),
                        "totalAmount", saved.getTotalAmount(), "currency", saved.getCurrency()));
        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> list(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        var results = "VENDOR".equals(principal.getRole())
                ? quotationRepository.findAllByOrganizationIdAndVendorIdIn(
                        organizationId, vendorIdsForUser(organizationId, principal.getUserId()),
                        PageRequest.of(page, size))
                : quotationRepository.findAllByOrganizationId(organizationId, PageRequest.of(page, size));
        return results.getContent().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public QuotationResponse get(UUID quotationId) {
        Quotation quote = requireQuotation(quotationId);
        requireVisibleToCurrentUser(quote);
        return response(quote);
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> forRfq(UUID rfqId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        Rfq rfq = rfqService.requireRfq(rfqId);
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        List<Quotation> quotations;
        if ("VENDOR".equals(principal.getRole())) {
            Set<UUID> vendorIds = vendorIdsForUser(organizationId, principal.getUserId());
            if (vendorIds.isEmpty()) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "VENDOR_MEMBERSHIP_REQUIRED",
                        "The authenticated user is not associated with a vendor");
            }
            if (!"PUBLISHED".equals(rfq.getStatus()) && !"IN_PROGRESS".equals(rfq.getStatus())
                    && !"CLOSED".equals(rfq.getStatus())) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "RFQ_NOT_FOUND", "RFQ not found");
            }
            quotations = quotationRepository.findAllByOrganizationIdAndRfqIdAndVendorIdInOrderByCreatedAt(
                    organizationId, rfqId, vendorIds);
        } else {
            quotations = quotationRepository.findAllByOrganizationIdAndRfqIdOrderByCreatedAt(
                    organizationId, rfqId);
        }
        return quotations.stream().map(this::response).toList();
    }

    @Transactional
    public QuotationResponse updateStatus(UUID quotationId, UpdateQuotationStatusRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Quotation quote = requireQuotation(quotationId);
        if (!Set.of("SUBMITTED", "UNDER_REVIEW").contains(quote.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "QUOTATION_NOT_REVIEWABLE",
                    "Only a submitted or under-review quotation can be evaluated");
        }
        if ("ACCEPTED".equals(request.status())
                && quotationRepository.existsByOrganizationIdAndRfqIdAndStatus(
                        organizationId, quote.getRfqId(), "ACCEPTED")) {
            throw new BusinessException(HttpStatus.CONFLICT, "RFQ_QUOTE_ALREADY_ACCEPTED",
                    "This RFQ already has an accepted quotation");
        }
        quote.updateStatus(request.status());
        quote.setUpdatedBy(principal.getUserId());
        Quotation saved = quotationRepository.save(quote);
        if ("ACCEPTED".equals(request.status())) {
            rfqService.markInProgress(quote.getRfqId(), principal.getUserId());
        }
        eventWriter.record("QUOTATION", saved.getId(), organizationId, "QUOTATION_STATUS_CHANGED",
                "procurax.quotation.v1", correlationId(rfqService.requireRfq(saved.getRfqId())),
                principal.getUserId(), Map.of("rfqId", saved.getRfqId(), "vendorId", saved.getVendorId(),
                        "status", saved.getStatus()));
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Quotation requireQuotation(UUID quotationId) {
        return quotationRepository.findByIdAndOrganizationId(
                        quotationId, organizationContext.currentOrganizationId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "QUOTATION_NOT_FOUND", "Quotation not found"));
    }

    private void requireVisibleToCurrentUser(Quotation quote) {
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        if ("VENDOR".equals(principal.getRole())
                && !vendorMembershipRepository.existsByOrganizationIdAndVendorIdAndUserId(
                        quote.getOrganizationId(), quote.getVendorId(), principal.getUserId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "QUOTATION_NOT_FOUND", "Quotation not found");
        }
    }

    private Set<UUID> vendorIdsForUser(UUID organizationId, UUID userId) {
        return vendorMembershipRepository.findAllByOrganizationIdAndUserId(organizationId, userId)
                .stream().map(VendorMembership::getVendorId).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private void validateOpenRfq(Rfq rfq) {
        if (!"PUBLISHED".equals(rfq.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, "RFQ_NOT_OPEN",
                    "Quotations can only be submitted to a published RFQ");
        }
        if (!rfq.getDeadline().isAfter(java.time.Instant.now())) {
            throw new BusinessException(HttpStatus.CONFLICT, "RFQ_DEADLINE_PASSED",
                    "The RFQ response deadline has passed");
        }
    }

    private BigDecimal validateAndCalculateTotal(List<RfqItem> rfqItems, List<QuotationItemRequest> quoteItems) {
        Map<UUID, RfqItem> expected = rfqItems.stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(RfqItem::getId, item -> item));
        Set<UUID> receivedIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (QuotationItemRequest item : quoteItems) {
            RfqItem requested = expected.get(item.rfqItemId());
            if (requested == null || !receivedIds.add(item.rfqItemId())) {
                throw badRequest("INVALID_QUOTATION_ITEMS", "Each quoted line must match one unique RFQ item");
            }
            if (requested.getQuantity() != item.quantity()) {
                throw badRequest("QUANTITY_MISMATCH", "Quotation item quantities must match the RFQ");
            }
            total = total.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        if (receivedIds.size() != expected.size()) {
            throw badRequest("INCOMPLETE_QUOTATION", "A quotation must price every RFQ item");
        }
        if (total.signum() <= 0) {
            throw badRequest("INVALID_QUOTATION_TOTAL", "Quotation total must be greater than zero");
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateCompliance(JsonNode compliance) {
        if (!compliance.isObject()) {
            throw badRequest("INVALID_COMPLIANCE", "Compliance information must be a JSON object");
        }
        for (String key : COMPLIANCE_KEYS) {
            if (!compliance.hasNonNull(key)) {
                throw badRequest("INVALID_COMPLIANCE", "Compliance field is required: " + key);
            }
        }
        for (String key : Set.of("isoCertification", "environmentalStandards", "documentSubmission")) {
            if (!compliance.get(key).isBoolean()) {
                throw badRequest("INVALID_COMPLIANCE", "Compliance field must be boolean: " + key);
            }
        }
        if (!Set.of("A+", "A", "B", "C").contains(compliance.path("materialGrade").asText())) {
            throw badRequest("INVALID_COMPLIANCE", "materialGrade must be A+, A, B, or C");
        }
    }

    private void saveItems(UUID organizationId, UUID quotationId, List<QuotationItemRequest> items) {
        itemRepository.saveAll(items.stream()
                .map(item -> new QuotationItem(organizationId, quotationId, item.rfqItemId(),
                        item.unitPrice(), item.quantity()))
                .toList());
    }

    private QuotationResponse response(Quotation quote) {
        UUID organizationId = organizationContext.currentOrganizationId();
        List<QuotationItemResponse> items = itemRepository
                .findAllByOrganizationIdAndQuotationIdOrderByCreatedAt(organizationId, quote.getId())
                .stream().map(QuotationItemResponse::from).toList();
        VendorScoreResponse score = scoreRepository.findAllByOrganizationIdAndQuotationId(
                        organizationId, quote.getId()).stream().findFirst()
                .map(VendorScoreResponse::from).orElse(null);
        return QuotationResponse.from(quote, items, score);
    }

    private UUID correlationId(Rfq rfq) {
        String current = CorrelationIdFilter.current();
        return current == null ? rfq.getCorrelationId() : UUID.fromString(current);
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }
}
