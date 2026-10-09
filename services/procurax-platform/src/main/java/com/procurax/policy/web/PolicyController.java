package com.procurax.policy.web;

import com.procurax.policy.service.PolicyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('POLICY_READ')")
    public List<PolicyRuleResponse> listRules() {
        return policyService.listRules();
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public PolicyRuleResponse createRule(@Valid @RequestBody PolicyRuleRequest request) {
        return policyService.createRule(request);
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('POLICY_MANAGE')")
    public PolicyRuleResponse updateRule(@PathVariable UUID id,
                                         @Valid @RequestBody PolicyRuleRequest request) {
        return policyService.updateRule(id, request);
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAuthority('POLICY_EVALUATE')")
    public PolicyEvaluationResponse evaluate(@Valid @RequestBody PurchasePolicyEvaluationRequest request) {
        return policyService.evaluate(request);
    }
}
