package com.procurax.policy.web;

import java.util.List;
import java.util.UUID;

public record PolicyEvaluationResponse(
        UUID evaluationId,
        String outcome,
        boolean requiresApproval,
        boolean policyPassed,
        List<PolicyDecision> decisions) {

    public record PolicyDecision(UUID ruleId, String ruleName, String outcome, String reason) {
    }
}
