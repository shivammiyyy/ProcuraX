package com.procurax.approval.web;

import com.procurax.approval.service.ApprovalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('APPROVAL_READ')")
    public List<ApprovalResponse> list() {
        return approvalService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPROVAL_READ')")
    public ApprovalResponse get(@PathVariable UUID id) {
        return approvalService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('APPROVAL_REQUEST')")
    public ApprovalResponse create(@Valid @RequestBody CreateApprovalRequest request) {
        return approvalService.create(request);
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('APPROVAL_DECIDE')")
    public ApprovalResponse decide(@PathVariable UUID id,
                                    @Valid @RequestBody ApprovalDecisionRequest request) {
        return approvalService.decide(id, request);
    }
}
