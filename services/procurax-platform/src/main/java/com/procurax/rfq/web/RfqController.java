package com.procurax.rfq.web;

import com.procurax.rfq.service.RfqService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rfqs")
public class RfqController {

    private final RfqService rfqService;

    public RfqController(RfqService rfqService) {
        this.rfqService = rfqService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('RFQ_READ')")
    public List<RfqResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return rfqService.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RFQ_READ')")
    public RfqResponse get(@PathVariable UUID id) {
        return rfqService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RFQ_CREATE')")
    public RfqResponse create(@Valid @RequestBody CreateRfqRequest request) {
        return rfqService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RFQ_UPDATE')")
    public RfqResponse update(@PathVariable UUID id, @Valid @RequestBody CreateRfqRequest request) {
        return rfqService.updateDraft(id, request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('RFQ_PUBLISH')")
    public RfqResponse publish(@PathVariable UUID id) {
        return rfqService.publish(id);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('RFQ_UPDATE')")
    public RfqResponse close(@PathVariable UUID id) {
        return rfqService.close(id);
    }
}
