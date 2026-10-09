package com.procurax.quotation.web;

import com.procurax.quotation.service.QuotationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping("/quotations")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    public List<QuotationResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return quotationService.list(page, size);
    }

    @GetMapping("/quotations/{id}")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    public QuotationResponse get(@PathVariable UUID id) {
        return quotationService.get(id);
    }

    @PostMapping("/quotations")
    @PreAuthorize("hasAuthority('QUOTE_SUBMIT')")
    public QuotationResponse create(@Valid @RequestBody CreateQuotationRequest request) {
        return quotationService.submit(request);
    }

    @PutMapping("/quotations/{id}")
    @PreAuthorize("hasAuthority('QUOTE_SUBMIT')")
    public QuotationResponse update(@PathVariable UUID id, @Valid @RequestBody CreateQuotationRequest request) {
        return quotationService.update(id, request);
    }

    @PatchMapping("/quotations/{id}/status")
    @PreAuthorize("hasAuthority('QUOTE_EVALUATE')")
    public QuotationResponse updateStatus(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateQuotationStatusRequest request) {
        return quotationService.updateStatus(id, request);
    }

    @GetMapping("/rfqs/{rfqId}/quotations")
    @PreAuthorize("hasAuthority('QUOTE_READ')")
    public List<QuotationResponse> forRfq(@PathVariable UUID rfqId) {
        return quotationService.forRfq(rfqId);
    }
}
