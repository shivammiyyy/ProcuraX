package com.procurax.payment.service;

import com.procurax.common.error.BusinessException;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.payment.domain.PaymentIntent;
import com.procurax.payment.domain.PaymentMandate;
import com.procurax.payment.domain.PaymentReceipt;
import com.procurax.payment.repository.PaymentIntentRepository;
import com.procurax.payment.repository.PaymentMandateRepository;
import com.procurax.payment.repository.PaymentReceiptRepository;
import com.procurax.payment.web.AuthorizePaymentRequest;
import com.procurax.payment.web.CreatePaymentMandateRequest;
import com.procurax.payment.web.PaymentMandateResponse;
import com.procurax.payment.web.PaymentResponse;
import com.procurax.purchaseorder.domain.PurchaseOrder;
import com.procurax.purchaseorder.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentMandateRepository mandateRepository;
    private final PaymentIntentRepository intentRepository;
    private final PaymentReceiptRepository receiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final OrganizationContext organizationContext;
    private final SandboxPaymentProcessor sandboxProcessor;
    private final OutboxEventWriter eventWriter;

    public PaymentService(PaymentMandateRepository mandateRepository,
                          PaymentIntentRepository intentRepository,
                          PaymentReceiptRepository receiptRepository,
                          PurchaseOrderRepository purchaseOrderRepository,
                          OrganizationContext organizationContext,
                          SandboxPaymentProcessor sandboxProcessor,
                          OutboxEventWriter eventWriter) {
        this.mandateRepository = mandateRepository;
        this.intentRepository = intentRepository;
        this.receiptRepository = receiptRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.organizationContext = organizationContext;
        this.sandboxProcessor = sandboxProcessor;
        this.eventWriter = eventWriter;
    }

    @Transactional
    public PaymentMandateResponse createMandate(CreatePaymentMandateRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        PurchaseOrder order = purchaseOrderRepository.findForPayment(organizationId, request.purchaseOrderId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "PURCHASE_ORDER_NOT_FOUND", "Purchase order not found"));
        if (!"ISSUED".equals(order.getStatus())) {
            throw conflict("PURCHASE_ORDER_NOT_PAYABLE", "Only an issued purchase order can be mandated");
        }
        if (request.expiresAt().isAfter(Instant.now().plusSeconds(366L * 24 * 60 * 60))) {
            throw badRequest("MANDATE_EXPIRATION_TOO_FAR",
                    "A sandbox mandate cannot expire more than 366 days from now");
        }
        if (intentRepository.findByOrganizationIdAndPurchaseOrderId(organizationId, order.getId()).isPresent()) {
            throw conflict("PAYMENT_ALREADY_STARTED",
                    "A payment has already been initiated for this purchase order");
        }
        if (mandateRepository.findByOrganizationIdAndPurchaseOrderIdAndStatus(
                organizationId, order.getId(), "ACTIVE").isPresent()) {
            throw conflict("ACTIVE_MANDATE_EXISTS",
                    "An active payment mandate already exists for this purchase order");
        }

        PaymentMandate mandate = new PaymentMandate(organizationId, order.getId(),
                order.getTotalAmount(), order.getCurrency(), request.expiresAt());
        mandate.setCreatedBy(principal.getUserId());
        PaymentMandate saved = mandateRepository.saveAndFlush(mandate);
        eventWriter.record("PAYMENT_MANDATE", saved.getId(), organizationId,
                "PAYMENT_MANDATE_CREATED", "procurax.payment.v1",
                OutboxEventWriter.currentCorrelationId(saved.getId()), principal.getUserId(),
                Map.of("mandateId", saved.getId(), "purchaseOrderId", order.getId(),
                        "maximumAmount", saved.getMaximumAmount(), "currency", saved.getCurrency(),
                        "expiresAt", saved.getExpiresAt()));
        return PaymentMandateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentMandateResponse> listMandates() {
        return mandateRepository.findAllByOrganizationIdOrderByCreatedAtDesc(
                        organizationContext.currentOrganizationId())
                .stream().map(PaymentMandateResponse::from).toList();
    }

    @Transactional
    public PaymentMandateResponse revokeMandate(UUID mandateId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        PaymentMandate mandate = mandateRepository.findForUpdate(organizationId, mandateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "PAYMENT_MANDATE_NOT_FOUND", "Payment mandate not found"));
        if (!"ACTIVE".equals(mandate.getStatus())) {
            throw conflict("PAYMENT_MANDATE_NOT_ACTIVE", "Only an active mandate can be revoked");
        }
        if (intentRepository.findByOrganizationIdAndPurchaseOrderId(
                organizationId, mandate.getPurchaseOrderId()).isPresent()) {
            throw conflict("PAYMENT_ALREADY_STARTED",
                    "A mandate cannot be revoked after payment authorization has started");
        }
        mandate.revoke();
        mandate.setUpdatedBy(principal.getUserId());
        PaymentMandate saved = mandateRepository.saveAndFlush(mandate);
        eventWriter.record("PAYMENT_MANDATE", saved.getId(), organizationId,
                "PAYMENT_MANDATE_REVOKED", "procurax.payment.v1",
                OutboxEventWriter.currentCorrelationId(saved.getId()), principal.getUserId(),
                Map.of("mandateId", saved.getId(), "purchaseOrderId", saved.getPurchaseOrderId()));
        return PaymentMandateResponse.from(saved);
    }

    @Transactional
    public PaymentResponse authorize(AuthorizePaymentRequest request, String idempotencyKey) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        PaymentIntent previousByKey = intentRepository.findByOrganizationIdAndAuthorizeIdempotencyKey(
                organizationId, idempotencyKey).orElse(null);
        if (previousByKey != null) {
            if (!previousByKey.getPurchaseOrderId().equals(request.purchaseOrderId())
                    || !previousByKey.getMandateId().equals(request.mandateId())) {
                throw conflict("IDEMPOTENCY_KEY_REUSED",
                        "The authorization idempotency key was already used for another request");
            }
            return response(previousByKey);
        }

        PurchaseOrder order = purchaseOrderRepository.findForPayment(organizationId, request.purchaseOrderId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "PURCHASE_ORDER_NOT_FOUND", "Purchase order not found"));
        if (!"ISSUED".equals(order.getStatus())) {
            throw conflict("PURCHASE_ORDER_NOT_PAYABLE", "Only an issued purchase order can be paid");
        }
        PaymentIntent existingForOrder = intentRepository.findByOrganizationIdAndPurchaseOrderId(
                organizationId, order.getId()).orElse(null);
        if (existingForOrder != null) {
            if (existingForOrder.getAuthorizeIdempotencyKey().equals(idempotencyKey)
                    && existingForOrder.getMandateId().equals(request.mandateId())) {
                return response(existingForOrder);
            }
            throw conflict("PAYMENT_ALREADY_STARTED",
                    "A payment has already been initiated for this purchase order");
        }

        PaymentMandate mandate = mandateRepository.findForUpdate(organizationId, request.mandateId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "PAYMENT_MANDATE_NOT_FOUND", "Payment mandate not found"));
        if (!mandate.getPurchaseOrderId().equals(order.getId())) {
            throw conflict("MANDATE_PURCHASE_ORDER_MISMATCH",
                    "The mandate is not scoped to this purchase order");
        }
        if (!"ACTIVE".equals(mandate.getStatus()) || !mandate.getExpiresAt().isAfter(Instant.now())) {
            throw conflict("PAYMENT_MANDATE_INACTIVE", "The payment mandate is revoked or expired");
        }
        if (mandate.getMaximumAmount().compareTo(order.getTotalAmount()) < 0
                || !mandate.getCurrency().equals(order.getCurrency())) {
            throw conflict("PAYMENT_MANDATE_LIMIT_EXCEEDED",
                    "The mandate does not cover the purchase order amount and currency");
        }

        String sandboxReference = sandboxProcessor.authorize();
        PaymentIntent intent = new PaymentIntent(organizationId, order.getId(), mandate.getId(),
                order.getTotalAmount(), order.getCurrency(), idempotencyKey, sandboxReference);
        intent.setCreatedBy(principal.getUserId());
        PaymentIntent saved = intentRepository.saveAndFlush(intent);
        saveReceipt(saved, "AUTHORIZED", sandboxReference, principal.getUserId());
        recordPaymentEvent(saved, "PAYMENT_AUTHORIZED", principal.getUserId());
        receiptRepository.flush();
        return response(saved);
    }

    @Transactional
    public PaymentResponse capture(UUID paymentId, String idempotencyKey) {
        return finish(paymentId, idempotencyKey, "CAPTURED");
    }

    @Transactional
    public PaymentResponse refund(UUID paymentId, String idempotencyKey) {
        return finish(paymentId, idempotencyKey, "REFUNDED");
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments() {
        return intentRepository.findAllByOrganizationIdOrderByCreatedAtDesc(
                        organizationContext.currentOrganizationId())
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        PaymentIntent intent = intentRepository.findByOrganizationIdAndId(organizationId, paymentId)
                .orElseThrow(this::paymentNotFound);
        return response(intent);
    }

    private PaymentResponse finish(UUID paymentId, String idempotencyKey, String operation) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        PaymentIntent intent = intentRepository.findForUpdate(organizationId, paymentId)
                .orElseThrow(this::paymentNotFound);
        String currentStatus = intent.getStatus();
        String requiredStatus = "CAPTURED".equals(operation) ? "AUTHORIZED" : "CAPTURED";
        String completedStatus = operation;
        String storedKey = "CAPTURED".equals(operation)
                ? intent.getCaptureIdempotencyKey() : intent.getRefundIdempotencyKey();
        if (completedStatus.equals(currentStatus)) {
            if (idempotencyKey.equals(storedKey)) {
                return response(intent);
            }
            throw conflict("PAYMENT_OPERATION_ALREADY_COMPLETED",
                    "This payment operation has already completed with another idempotency key");
        }
        if (!requiredStatus.equals(currentStatus)) {
            throw conflict("INVALID_PAYMENT_STATE",
                    "Payment must be " + requiredStatus + " before it can be " + operation);
        }
        PaymentIntent previousByKey = "CAPTURED".equals(operation)
                ? intentRepository.findByOrganizationIdAndCaptureIdempotencyKey(organizationId, idempotencyKey)
                        .orElse(null)
                : intentRepository.findByOrganizationIdAndRefundIdempotencyKey(organizationId, idempotencyKey)
                        .orElse(null);
        if (previousByKey != null && !previousByKey.getId().equals(intent.getId())) {
            throw conflict("IDEMPOTENCY_KEY_REUSED",
                    "The payment idempotency key was already used for another payment");
        }

        String sandboxReference;
        if ("CAPTURED".equals(operation)) {
            sandboxReference = sandboxProcessor.capture();
            intent.capture(idempotencyKey, sandboxReference);
        } else {
            sandboxReference = sandboxProcessor.refund();
            intent.refund(idempotencyKey, sandboxReference);
        }
        intent.setUpdatedBy(principal.getUserId());
        PaymentIntent saved = intentRepository.saveAndFlush(intent);
        saveReceipt(saved, operation, sandboxReference, principal.getUserId());
        recordPaymentEvent(saved, "PAYMENT_" + operation, principal.getUserId());
        receiptRepository.flush();
        return response(saved);
    }

    private void saveReceipt(PaymentIntent intent, String operation,
                             String sandboxReference, UUID actorId) {
        PaymentReceipt receipt = new PaymentReceipt(intent.getOrganizationId(), intent.getId(),
                operation, sandboxReference, intent.getAmount(), intent.getCurrency());
        receipt.setCreatedBy(actorId);
        receiptRepository.save(receipt);
    }

    private void recordPaymentEvent(PaymentIntent intent, String eventType, UUID actorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentIntentId", intent.getId());
        payload.put("purchaseOrderId", intent.getPurchaseOrderId());
        payload.put("mandateId", intent.getMandateId());
        payload.put("amount", intent.getAmount());
        payload.put("currency", intent.getCurrency());
        payload.put("status", intent.getStatus());
        eventWriter.record("PAYMENT", intent.getId(), intent.getOrganizationId(),
                eventType, "procurax.payment.v1",
                OutboxEventWriter.currentCorrelationId(intent.getId()), actorId, payload);
    }

    private PaymentResponse response(PaymentIntent intent) {
        return PaymentResponse.from(intent,
                receiptRepository.findAllByOrganizationIdAndPaymentIntentIdOrderByCreatedAt(
                        intent.getOrganizationId(), intent.getId()));
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private BusinessException paymentNotFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found");
    }
}
