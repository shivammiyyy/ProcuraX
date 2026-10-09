package com.procurax.payment.web;

import com.procurax.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payment-mandates")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    public List<PaymentMandateResponse> listMandates() {
        return paymentService.listMandates();
    }

    @PostMapping("/payment-mandates")
    @PreAuthorize("hasAuthority('PAYMENT_MANDATE_MANAGE')")
    public PaymentMandateResponse createMandate(@Valid @RequestBody CreatePaymentMandateRequest request) {
        return paymentService.createMandate(request);
    }

    @PostMapping("/payment-mandates/{id}/revoke")
    @PreAuthorize("hasAuthority('PAYMENT_MANDATE_MANAGE')")
    public PaymentMandateResponse revokeMandate(@PathVariable UUID id) {
        return paymentService.revokeMandate(id);
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    public List<PaymentResponse> listPayments() {
        return paymentService.listPayments();
    }

    @GetMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('PAYMENT_READ')")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }

    @PostMapping("/payments/authorize")
    @PreAuthorize("hasAuthority('PAYMENT_AUTHORIZE')")
    public PaymentResponse authorize(@Valid @RequestBody AuthorizePaymentRequest request,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9._:-]{16,100}") String idempotencyKey) {
        return paymentService.authorize(request, idempotencyKey);
    }

    @PostMapping("/payments/{id}/capture")
    @PreAuthorize("hasAuthority('PAYMENT_CAPTURE')")
    public PaymentResponse capture(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9._:-]{16,100}") String idempotencyKey) {
        return paymentService.capture(id, idempotencyKey);
    }

    @PostMapping("/payments/{id}/refund")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND')")
    public PaymentResponse refund(@PathVariable UUID id,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9._:-]{16,100}") String idempotencyKey) {
        return paymentService.refund(id, idempotencyKey);
    }
}
