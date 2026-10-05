package com.procurax.vendor.web;

import com.procurax.vendor.service.VendorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public List<VendorSummary> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return vendorService.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VENDOR_READ')")
    public VendorSummary get(@PathVariable UUID id) {
        return vendorService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('VENDOR_CREATE')")
    public VendorSummary create(@Valid @RequestBody CreateVendorRequest request) {
        return vendorService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('VENDOR_UPDATE')")
    public VendorSummary update(@PathVariable UUID id, @Valid @RequestBody UpdateVendorRequest request) {
        return vendorService.update(id, request);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public Map<String, String> addMember(@PathVariable UUID id, @Valid @RequestBody AddVendorMemberRequest request) {
        vendorService.addMember(id, request.userId());
        return Map.of("status", "MEMBER_ADDED");
    }
}
