package com.procurax.fulfillment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurax.common.error.BusinessException;
import com.procurax.fulfillment.domain.Invoice;
import com.procurax.fulfillment.domain.ReconciliationRun;
import com.procurax.fulfillment.domain.Shipment;
import com.procurax.fulfillment.domain.ShipmentItem;
import com.procurax.fulfillment.repository.InvoiceRepository;
import com.procurax.fulfillment.repository.ReconciliationRunRepository;
import com.procurax.fulfillment.repository.ShipmentItemRepository;
import com.procurax.fulfillment.repository.ShipmentRepository;
import com.procurax.fulfillment.web.CreateInvoiceRequest;
import com.procurax.fulfillment.web.CreateReconciliationRequest;
import com.procurax.fulfillment.web.CreateShipmentRequest;
import com.procurax.fulfillment.web.InvoiceResponse;
import com.procurax.fulfillment.web.ReconciliationResponse;
import com.procurax.fulfillment.web.ShipmentResponse;
import com.procurax.fulfillment.web.ShipmentStatusRequest;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.payment.domain.PaymentIntent;
import com.procurax.payment.domain.PaymentReceipt;
import com.procurax.payment.repository.PaymentIntentRepository;
import com.procurax.payment.repository.PaymentReceiptRepository;
import com.procurax.purchaseorder.domain.PurchaseOrder;
import com.procurax.purchaseorder.domain.PurchaseOrderItem;
import com.procurax.purchaseorder.repository.PurchaseOrderItemRepository;
import com.procurax.purchaseorder.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
public class FulfillmentService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final OrganizationContext organizationContext;
    private final ObjectMapper objectMapper;
    private final OutboxEventWriter eventWriter;

    public FulfillmentService(ShipmentRepository shipmentRepository,
                              ShipmentItemRepository shipmentItemRepository,
                              InvoiceRepository invoiceRepository,
                              ReconciliationRunRepository reconciliationRunRepository,
                              PurchaseOrderRepository purchaseOrderRepository,
                              PurchaseOrderItemRepository purchaseOrderItemRepository,
                              PaymentIntentRepository paymentIntentRepository,
                              PaymentReceiptRepository paymentReceiptRepository,
                              OrganizationContext organizationContext,
                              ObjectMapper objectMapper,
                              OutboxEventWriter eventWriter) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentItemRepository = shipmentItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.organizationContext = organizationContext;
        this.objectMapper = objectMapper;
        this.eventWriter = eventWriter;
    }

    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        PurchaseOrder order = purchaseOrderRepository.findForPayment(organizationId, request.purchaseOrderId())
                .orElseThrow(this::purchaseOrderNotFound);
        if (!"ISSUED".equals(order.getStatus())) {
            throw conflict("PURCHASE_ORDER_NOT_FULFILLABLE", "Only an issued purchase order can be shipped");
        }
        String trackingNumber = request.trackingNumber().trim();
        if (shipmentRepository.existsByOrganizationIdAndTrackingNumber(organizationId, trackingNumber)) {
            throw conflict("TRACKING_NUMBER_EXISTS", "The tracking number is already registered");
        }

        List<PurchaseOrderItem> orderItems =
                purchaseOrderItemRepository.findAllByOrganizationIdAndPurchaseOrderIdOrderByCreatedAt(
                        organizationId, order.getId());
        Map<UUID, PurchaseOrderItem> itemsById = new HashMap<>();
        orderItems.forEach(item -> itemsById.put(item.getId(), item));
        if (itemsById.isEmpty()) {
            throw conflict("PURCHASE_ORDER_ITEMS_REQUIRED", "The purchase order has no item lines");
        }
        Set<UUID> requestedIds = new HashSet<>();
        Map<UUID, Integer> alreadyShippedByItem = new HashMap<>();
        shipmentItemRepository.findAllByOrganizationIdAndPurchaseOrderId(organizationId, order.getId())
                .forEach(existing -> alreadyShippedByItem.merge(
                        existing.getPurchaseOrderItemId(), existing.getQuantity(), Integer::sum));
        for (var requested : request.items()) {
            if (!requestedIds.add(requested.purchaseOrderItemId())) {
                throw badRequest("DUPLICATE_SHIPMENT_ITEM", "A purchase order line may appear only once per shipment");
            }
            PurchaseOrderItem item = itemsById.get(requested.purchaseOrderItemId());
            if (item == null) {
                throw badRequest("PURCHASE_ORDER_ITEM_NOT_FOUND",
                        "Every shipment line must reference an item from this purchase order");
            }
            int alreadyShipped = alreadyShippedByItem.getOrDefault(item.getId(), 0);
            if (requested.quantity() > item.getQuantity() - alreadyShipped) {
                throw conflict("SHIPMENT_QUANTITY_EXCEEDED",
                        "Shipment quantity exceeds the remaining ordered quantity");
            }
        }

        Shipment shipment = new Shipment(organizationId, order.getId(), trackingNumber,
                request.carrier().trim(), request.expectedAt());
        shipment.setCreatedBy(principal.getUserId());
        Shipment saved = shipmentRepository.saveAndFlush(shipment);
        List<ShipmentItem> lines = request.items().stream()
                .map(line -> new ShipmentItem(organizationId, saved.getId(), order.getId(),
                        line.purchaseOrderItemId(), itemsById.get(line.purchaseOrderItemId()).getDescription(),
                        line.quantity()))
                .peek(line -> line.setCreatedBy(principal.getUserId()))
                .toList();
        shipmentItemRepository.saveAll(lines);
        eventWriter.record("SHIPMENT", saved.getId(), organizationId, "SHIPMENT_RECORDED",
                "procurax.fulfillment.v1", OutboxEventWriter.currentCorrelationId(saved.getId()),
                principal.getUserId(), Map.of("shipmentId", saved.getId(), "purchaseOrderId", order.getId(),
                        "trackingNumber", trackingNumber, "itemCount", lines.size()));
        shipmentItemRepository.flush();
        return ShipmentResponse.from(saved, lines);
    }

    @Transactional
    public ShipmentResponse updateShipmentStatus(UUID shipmentId, ShipmentStatusRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        UUID actorId = organizationContext.currentPrincipal().getUserId();
        Shipment shipment = shipmentRepository.findForUpdate(organizationId, shipmentId)
                .orElseThrow(this::shipmentNotFound);
        if ("DELIVERED".equals(request.status())) {
            if (!"IN_TRANSIT".equals(shipment.getStatus())) {
                throw conflict("SHIPMENT_NOT_IN_TRANSIT",
                        "Only an in-transit shipment can be updated");
            }
            if (request.exceptionReason() != null && !request.exceptionReason().isBlank()) {
                throw badRequest("UNEXPECTED_EXCEPTION_REASON",
                        "An exception reason is only valid for an exception status");
            }
            shipment.deliver();
        } else {
            if (!"IN_TRANSIT".equals(shipment.getStatus())) {
                throw conflict("SHIPMENT_NOT_IN_TRANSIT",
                        "Only an in-transit shipment can be updated");
            }
            if (request.exceptionReason() == null || request.exceptionReason().isBlank()) {
                throw badRequest("SHIPMENT_EXCEPTION_REASON_REQUIRED",
                        "An exception status requires a reason");
            }
            shipment.reportException(request.exceptionReason().trim());
        }
        shipment.setUpdatedBy(actorId);
        Shipment saved = shipmentRepository.saveAndFlush(shipment);
        eventWriter.record("SHIPMENT", saved.getId(), organizationId,
                "SHIPMENT_" + saved.getStatus(), "procurax.fulfillment.v1",
                OutboxEventWriter.currentCorrelationId(saved.getId()), actorId,
                Map.of("shipmentId", saved.getId(), "purchaseOrderId", saved.getPurchaseOrderId(),
                        "status", saved.getStatus(), "exceptionReason",
                        saved.getExceptionReason() == null ? "" : saved.getExceptionReason()));
        return shipmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> listShipments(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        return shipmentRepository.findAllByOrganizationIdOrderByCreatedAtDesc(
                        organizationId, PageRequest.of(page, size))
                .getContent().stream().map(this::shipmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(UUID shipmentId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        Shipment shipment = shipmentRepository.findByOrganizationIdAndId(organizationId, shipmentId)
                .orElseThrow(this::shipmentNotFound);
        return shipmentResponse(shipment);
    }

    @Transactional
    public InvoiceResponse recordInvoice(CreateInvoiceRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        UUID actorId = organizationContext.currentPrincipal().getUserId();
        PurchaseOrder order = purchaseOrderRepository.findForPayment(organizationId, request.purchaseOrderId())
                .orElseThrow(this::purchaseOrderNotFound);
        if (!"ISSUED".equals(order.getStatus())) {
            throw conflict("PURCHASE_ORDER_NOT_INVOICEABLE",
                    "Only an issued purchase order can be invoiced");
        }
        if (!order.getCurrency().equals(request.currency())) {
            throw badRequest("INVOICE_CURRENCY_MISMATCH",
                    "Invoice currency must match the purchase order");
        }
        if (request.dueDate() != null && request.dueDate().isBefore(request.invoiceDate())) {
            throw badRequest("INVALID_INVOICE_DUE_DATE", "Invoice due date cannot precede invoice date");
        }
        if (invoiceRepository.findByOrganizationIdAndPurchaseOrderId(organizationId, order.getId()).isPresent()) {
            throw conflict("INVOICE_ALREADY_RECORDED", "An invoice is already recorded for this purchase order");
        }
        String invoiceNumber = request.invoiceNumber().trim();
        if (invoiceRepository.existsByOrganizationIdAndInvoiceNumberIgnoreCase(
                organizationId, invoiceNumber)) {
            throw conflict("INVOICE_NUMBER_EXISTS", "The invoice number is already recorded");
        }
        Invoice invoice = new Invoice(organizationId, order.getId(), invoiceNumber,
                request.invoiceDate(), request.dueDate(), request.amount(), request.currency());
        invoice.setCreatedBy(actorId);
        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        eventWriter.record("INVOICE", saved.getId(), organizationId, "INVOICE_RECORDED",
                "procurax.fulfillment.v1", OutboxEventWriter.currentCorrelationId(saved.getId()),
                actorId, Map.of("invoiceId", saved.getId(), "purchaseOrderId", order.getId(),
                        "invoiceNumber", saved.getInvoiceNumber(), "amount", saved.getAmount(),
                        "currency", saved.getCurrency()));
        return InvoiceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        return invoiceRepository.findAllByOrganizationIdOrderByCreatedAtDesc(
                        organizationId, PageRequest.of(page, size))
                .getContent().stream().map(InvoiceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByOrganizationIdAndId(
                        organizationContext.currentOrganizationId(), invoiceId)
                .orElseThrow(this::invoiceNotFound);
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public ReconciliationResponse reconcile(CreateReconciliationRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        UUID actorId = organizationContext.currentPrincipal().getUserId();
        Invoice invoice = invoiceRepository.findByOrganizationIdAndId(organizationId, request.invoiceId())
                .orElseThrow(this::invoiceNotFound);
        PurchaseOrder order = purchaseOrderRepository.findByOrganizationIdAndId(
                        organizationId, invoice.getPurchaseOrderId())
                .orElseThrow(this::purchaseOrderNotFound);
        List<ReconciliationFinding> findings = new ArrayList<>();
        if (invoice.getAmount().compareTo(order.getTotalAmount()) != 0
                || !invoice.getCurrency().equals(order.getCurrency())) {
            findings.add(new ReconciliationFinding("INVOICE_MISMATCH",
                    "Invoice amount or currency does not match the purchase order"));
        }

        List<PurchaseOrderItem> orderLines =
                purchaseOrderItemRepository.findAllByOrganizationIdAndPurchaseOrderIdOrderByCreatedAt(
                        organizationId, order.getId());
        Map<UUID, Integer> requiredQuantities = new HashMap<>();
        orderLines.forEach(line -> requiredQuantities.put(line.getId(), line.getQuantity()));
        Map<UUID, Integer> deliveredQuantities = new HashMap<>();
        for (Shipment shipment : shipmentRepository.findAllByOrganizationIdAndPurchaseOrderIdOrderByCreatedAt(
                organizationId, order.getId())) {
            if ("EXCEPTION".equals(shipment.getStatus())) {
                findings.add(new ReconciliationFinding("SHIPMENT_EXCEPTION",
                        "Shipment " + shipment.getTrackingNumber() + " has an unresolved delivery exception"));
            }
            if ("DELIVERED".equals(shipment.getStatus())) {
                shipmentItemRepository.findAllByOrganizationIdAndShipmentIdOrderByCreatedAt(
                                organizationId, shipment.getId())
                        .forEach(line -> deliveredQuantities.merge(
                                line.getPurchaseOrderItemId(), line.getQuantity(), Integer::sum));
            }
        }
        if (orderLines.isEmpty() || requiredQuantities.entrySet().stream()
                .anyMatch(entry -> deliveredQuantities.getOrDefault(entry.getKey(), 0) < entry.getValue())) {
            findings.add(new ReconciliationFinding("FULFILLMENT_INCOMPLETE",
                    "Delivered shipment quantities do not cover every purchase order line"));
        }
        if (deliveredQuantities.entrySet().stream()
                .anyMatch(entry -> entry.getValue() > requiredQuantities.getOrDefault(entry.getKey(), 0))) {
            findings.add(new ReconciliationFinding("FULFILLMENT_OVER_DELIVERED",
                    "Delivered shipment quantities exceed one or more purchase order lines"));
        }

        PaymentIntent payment = paymentIntentRepository
                .findByOrganizationIdAndPurchaseOrderId(organizationId, order.getId()).orElse(null);
        if (payment == null) {
            findings.add(new ReconciliationFinding("PAYMENT_NOT_FOUND",
                    "No sandbox payment intent is recorded for the purchase order"));
        } else {
            if (!"CAPTURED".equals(payment.getStatus())) {
                findings.add(new ReconciliationFinding("PAYMENT_NOT_SETTLED",
                        "Sandbox payment state is " + payment.getStatus() + ", not CAPTURED"));
            }
            if (payment.getAmount().compareTo(order.getTotalAmount()) != 0
                    || !payment.getCurrency().equals(order.getCurrency())) {
                findings.add(new ReconciliationFinding("PAYMENT_MISMATCH",
                        "Captured payment amount or currency does not match the purchase order"));
            }
            PaymentReceipt capturedReceipt = paymentReceiptRepository
                    .findAllByOrganizationIdAndPaymentIntentIdOrderByCreatedAt(organizationId, payment.getId())
                    .stream().filter(receipt -> "CAPTURED".equals(receipt.getOperation()))
                    .findFirst().orElse(null);
            if (capturedReceipt == null
                    || capturedReceipt.getAmount().compareTo(order.getTotalAmount()) != 0
                    || !capturedReceipt.getCurrency().equals(order.getCurrency())) {
                findings.add(new ReconciliationFinding("CAPTURE_RECEIPT_MISMATCH",
                        "No matching sandbox capture receipt is recorded"));
            }
        }

        String status = findings.isEmpty() ? "MATCHED" : "EXCEPTION";
        ReconciliationRun run = new ReconciliationRun(organizationId, order.getId(), invoice.getId(),
                payment == null ? null : payment.getId(), status, objectMapper.valueToTree(findings));
        run.setCreatedBy(actorId);
        ReconciliationRun saved = reconciliationRunRepository.saveAndFlush(run);
        eventWriter.record("RECONCILIATION", saved.getId(), organizationId,
                "RECONCILIATION_" + status, "procurax.fulfillment.v1",
                OutboxEventWriter.currentCorrelationId(saved.getId()), actorId,
                Map.of("reconciliationId", saved.getId(), "purchaseOrderId", order.getId(),
                        "invoiceId", invoice.getId(), "paymentIntentId",
                        payment == null ? "" : payment.getId().toString(),
                        "status", status, "findingCount", findings.size()));
        return ReconciliationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponse> listReconciliations(int page, int size) {
        UUID organizationId = organizationContext.currentOrganizationId();
        return reconciliationRunRepository.findAllByOrganizationIdOrderByReconciledAtDesc(
                        organizationId, PageRequest.of(page, size))
                .getContent().stream().map(ReconciliationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse getReconciliation(UUID id) {
        ReconciliationRun run = reconciliationRunRepository.findByOrganizationIdAndId(
                        organizationContext.currentOrganizationId(), id)
                .orElseThrow(this::reconciliationNotFound);
        return ReconciliationResponse.from(run);
    }

    private ShipmentResponse shipmentResponse(Shipment shipment) {
        return ShipmentResponse.from(shipment, shipmentItemRepository
                .findAllByOrganizationIdAndShipmentIdOrderByCreatedAt(
                        shipment.getOrganizationId(), shipment.getId()));
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private BusinessException purchaseOrderNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND", "Purchase order not found");
    }

    private BusinessException shipmentNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", "Shipment not found");
    }

    private BusinessException invoiceNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found");
    }

    private BusinessException reconciliationNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "RECONCILIATION_NOT_FOUND",
                "Reconciliation run not found");
    }

    private record ReconciliationFinding(String code, String detail) {
    }
}
