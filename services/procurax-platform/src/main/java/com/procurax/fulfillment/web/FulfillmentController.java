package com.procurax.fulfillment.web;

import com.procurax.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/shipments")
    @PreAuthorize("hasAuthority('SHIPMENT_READ')")
    public List<ShipmentResponse> listShipments(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return fulfillmentService.listShipments(page, size);
    }

    @GetMapping("/shipments/{id}")
    @PreAuthorize("hasAuthority('SHIPMENT_READ')")
    public ShipmentResponse getShipment(@PathVariable UUID id) {
        return fulfillmentService.getShipment(id);
    }

    @PostMapping("/shipments")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    public ShipmentResponse createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        return fulfillmentService.createShipment(request);
    }

    @PostMapping("/shipments/{id}/status")
    @PreAuthorize("hasAuthority('SHIPMENT_MANAGE')")
    public ShipmentResponse updateShipmentStatus(@PathVariable UUID id,
                                                 @Valid @RequestBody ShipmentStatusRequest request) {
        return fulfillmentService.updateShipmentStatus(id, request);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('INVOICE_READ')")
    public List<InvoiceResponse> listInvoices(@RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return fulfillmentService.listInvoices(page, size);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('INVOICE_READ')")
    public InvoiceResponse getInvoice(@PathVariable UUID id) {
        return fulfillmentService.getInvoice(id);
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAuthority('INVOICE_MANAGE')")
    public InvoiceResponse recordInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return fulfillmentService.recordInvoice(request);
    }

    @GetMapping("/reconciliations")
    @PreAuthorize("hasAuthority('RECONCILIATION_READ')")
    public List<ReconciliationResponse> listReconciliations(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return fulfillmentService.listReconciliations(page, size);
    }

    @GetMapping("/reconciliations/{id}")
    @PreAuthorize("hasAuthority('RECONCILIATION_READ')")
    public ReconciliationResponse getReconciliation(@PathVariable UUID id) {
        return fulfillmentService.getReconciliation(id);
    }

    @PostMapping("/reconciliations")
    @PreAuthorize("hasAuthority('RECONCILIATION_EXECUTE')")
    public ReconciliationResponse reconcile(@Valid @RequestBody CreateReconciliationRequest request) {
        return fulfillmentService.reconcile(request);
    }
}
