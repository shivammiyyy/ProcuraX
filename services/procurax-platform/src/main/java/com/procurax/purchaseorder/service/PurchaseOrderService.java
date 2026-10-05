package com.procurax.purchaseorder.service;

import com.procurax.approval.domain.ApprovalRequest;
import com.procurax.approval.domain.ApprovalStatus;
import com.procurax.approval.repository.ApprovalRequestRepository;
import com.procurax.common.error.BusinessException;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.purchaseorder.domain.PurchaseOrder;
import com.procurax.purchaseorder.domain.PurchaseOrderItem;
import com.procurax.purchaseorder.repository.PurchaseOrderItemRepository;
import com.procurax.purchaseorder.repository.PurchaseOrderRepository;
import com.procurax.purchaseorder.web.CreatePurchaseOrderRequest;
import com.procurax.purchaseorder.web.PurchaseOrderResponse;
import com.procurax.quotation.domain.Quotation;
import com.procurax.quotation.domain.QuotationItem;
import com.procurax.quotation.repository.QuotationItemRepository;
import com.procurax.quotation.repository.QuotationRepository;
import com.procurax.rfq.domain.Rfq;
import com.procurax.rfq.domain.RfqItem;
import com.procurax.rfq.repository.RfqItemRepository;
import com.procurax.rfq.service.RfqService;
import com.procurax.vendor.domain.Vendor;
import com.procurax.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final RfqItemRepository rfqItemRepository;
    private final VendorRepository vendorRepository;
    private final RfqService rfqService;
    private final OrganizationContext organizationContext;
    private final OutboxEventWriter eventWriter;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                PurchaseOrderItemRepository purchaseOrderItemRepository,
                                ApprovalRequestRepository approvalRequestRepository,
                                QuotationRepository quotationRepository,
                                QuotationItemRepository quotationItemRepository,
                                RfqItemRepository rfqItemRepository,
                                VendorRepository vendorRepository,
                                RfqService rfqService,
                                OrganizationContext organizationContext,
                                OutboxEventWriter eventWriter) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.quotationRepository = quotationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.rfqItemRepository = rfqItemRepository;
        this.vendorRepository = vendorRepository;
        this.rfqService = rfqService;
        this.organizationContext = organizationContext;
        this.eventWriter = eventWriter;
    }

    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        Quotation quotation = quotationRepository.findForPurchaseOrder(request.quotationId(), organizationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "QUOTATION_NOT_FOUND", "Quotation not found"));
        if (!"ACCEPTED".equals(quotation.getStatus())) {
            throw conflict("QUOTATION_NOT_ACCEPTED", "A purchase order requires an accepted quotation");
        }

        ApprovalRequest approval = approvalRequestRepository.findForUpdate(
                        organizationId, request.approvalRequestId())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "APPROVAL_NOT_FOUND", "Approval request not found"));
        if (approval.getStatus() != ApprovalStatus.APPROVED) {
            throw conflict("APPROVAL_NOT_GRANTED", "A purchase order requires an approved request");
        }
        if (!quotation.getId().equals(approval.getQuotationId())
                || !quotation.getRfqId().equals(approval.getRfqId())) {
            throw conflict("APPROVAL_PURCHASING_BASIS_MISMATCH",
                    "The approved request does not authorize this quotation");
        }
        if (approval.getAmount().compareTo(quotation.getTotalAmount()) != 0
                || !approval.getCurrency().equals(quotation.getCurrency())) {
            throw conflict("APPROVAL_PURCHASING_BASIS_MISMATCH",
                    "The approved amount and currency do not match the accepted quotation");
        }
        if (purchaseOrderRepository.existsByOrganizationIdAndApprovalRequestId(
                    organizationId, approval.getId())
                || purchaseOrderRepository.existsByOrganizationIdAndQuotationId(
                    organizationId, quotation.getId())) {
            throw conflict("PURCHASE_ORDER_ALREADY_EXISTS",
                    "A purchase order already exists for this approval or quotation");
        }

        Rfq rfq = rfqService.requireRfq(quotation.getRfqId());
        if (!"IN_PROGRESS".equals(rfq.getStatus())
                || !rfq.getCategory().equalsIgnoreCase(approval.getCategory())) {
            throw conflict("RFQ_NOT_AWARDABLE",
                    "The approved request and quotation do not match an awardable RFQ");
        }
        Vendor vendor = vendorRepository.findByIdAndOrganizationId(quotation.getVendorId(), organizationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND", "Vendor not found"));

        List<PurchaseOrderItem> items = snapshotItems(organizationId, quotation, rfq);
        BigDecimal lineTotal = items.stream().map(PurchaseOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lineTotal.compareTo(quotation.getTotalAmount()) != 0) {
            throw conflict("QUOTATION_TOTAL_MISMATCH",
                    "The accepted quotation total does not match its itemized prices");
        }

        PurchaseOrder order = new PurchaseOrder(organizationId, "PO-" + UUID.randomUUID(),
                approval.getId(), rfq.getId(), quotation.getId(), vendor.getId(),
                vendor.getName(), vendor.getContactEmail(), quotation.getTotalAmount(),
                quotation.getCurrency(), quotation.getDeliveryDays(), quotation.getPaymentTermsDays());
        order.setCreatedBy(principal.getUserId());
        PurchaseOrder saved = purchaseOrderRepository.saveAndFlush(order);
        List<PurchaseOrderItem> persistedItems = items.stream()
                .map(item -> new PurchaseOrderItem(organizationId, saved.getId(), item.getRfqItemId(),
                        item.getDescription(), item.getSpecification(), item.getQuantity(), item.getUnit(),
                        item.getUnitPrice(), item.getLineTotal()))
                .peek(item -> item.setCreatedBy(principal.getUserId()))
                .toList();
        purchaseOrderItemRepository.saveAll(persistedItems);

        Map<String, Object> payload = new HashMap<>();
        payload.put("purchaseOrderId", saved.getId());
        payload.put("poNumber", saved.getPoNumber());
        payload.put("approvalRequestId", approval.getId());
        payload.put("rfqId", rfq.getId());
        payload.put("quotationId", quotation.getId());
        payload.put("vendorId", vendor.getId());
        payload.put("totalAmount", saved.getTotalAmount());
        payload.put("currency", saved.getCurrency());
        payload.put("status", saved.getStatus());
        eventWriter.record("PURCHASE_ORDER", saved.getId(), organizationId,
                "PURCHASE_ORDER_ISSUED", "procurax.purchase-order.v1",
                OutboxEventWriter.currentCorrelationId(saved.getId()), principal.getUserId(), payload);
        purchaseOrderItemRepository.flush();
        return PurchaseOrderResponse.from(saved, persistedItems);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> list(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        return purchaseOrderRepository.findAllByOrganizationIdOrderByCreatedAtDesc(
                        organizationId, PageRequest.of(page, size))
                .getContent().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(UUID id) {
        UUID organizationId = organizationContext.currentOrganizationId();
        PurchaseOrder order = purchaseOrderRepository.findByOrganizationIdAndId(organizationId, id)
                .orElseThrow(this::notFound);
        return response(order);
    }

    private List<PurchaseOrderItem> snapshotItems(UUID organizationId, Quotation quotation, Rfq rfq) {
        List<QuotationItem> quoteItems = quotationItemRepository
                .findAllByOrganizationIdAndQuotationIdOrderByCreatedAt(organizationId, quotation.getId());
        if (quoteItems.isEmpty()) {
            throw conflict("QUOTATION_ITEMS_REQUIRED",
                    "A purchase order requires an itemized accepted quotation");
        }
        Map<UUID, RfqItem> requestedItems = new HashMap<>();
        for (RfqItem item : rfqItemRepository.findAllByOrganizationIdAndRfqIdOrderByCreatedAt(
                organizationId, rfq.getId())) {
            requestedItems.put(item.getId(), item);
        }
        if (requestedItems.isEmpty() || quoteItems.size() != requestedItems.size()) {
            throw conflict("QUOTATION_ITEMS_INVALID",
                    "Accepted quotation must price every RFQ item exactly once");
        }
        Set<UUID> quotedRfqItems = new HashSet<>();
        List<PurchaseOrderItem> snapshots = quoteItems.stream().map(quoteItem -> {
            RfqItem rfqItem = requestedItems.get(quoteItem.getRfqItemId());
            if (rfqItem == null || !quotedRfqItems.add(rfqItem.getId())
                    || rfqItem.getQuantity() != quoteItem.getQuantity()) {
                throw conflict("QUOTATION_ITEMS_INVALID",
                        "Accepted quotation items must match each RFQ item and quantity exactly once");
            }
            BigDecimal itemTotal = quoteItem.getUnitPrice().multiply(BigDecimal.valueOf(quoteItem.getQuantity()));
            return new PurchaseOrderItem(organizationId, null, rfqItem.getId(),
                    rfqItem.getDescription(), rfqItem.getSpecification(), quoteItem.getQuantity(),
                    rfqItem.getUnit(), quoteItem.getUnitPrice(), itemTotal);
        }).toList();
        if (quotedRfqItems.size() != requestedItems.size()) {
            throw conflict("QUOTATION_ITEMS_INVALID",
                    "Accepted quotation must price every RFQ item exactly once");
        }
        return snapshots;
    }

    private PurchaseOrderResponse response(PurchaseOrder order) {
        return PurchaseOrderResponse.from(order,
                purchaseOrderItemRepository.findAllByOrganizationIdAndPurchaseOrderIdOrderByCreatedAt(
                        order.getOrganizationId(), order.getId()));
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND",
                "Purchase order not found");
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
