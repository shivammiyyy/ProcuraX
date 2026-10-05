package com.procurax.approval.web;

import com.procurax.approval.domain.ApprovalStep;
import java.time.Instant;
import java.util.UUID;

public record ApprovalStepResponse(
        int stepOrder,
        UUID approverUserId,
        String status,
        String decisionComment,
        Instant decidedAt) {

    public static ApprovalStepResponse from(ApprovalStep step) {
        return new ApprovalStepResponse(step.getStepOrder(), step.getApproverUserId(),
                step.getStatus().name(), step.getDecisionComment(), step.getDecidedAt());
    }
}
