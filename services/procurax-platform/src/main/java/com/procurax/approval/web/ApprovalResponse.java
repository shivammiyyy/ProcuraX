package com.procurax.approval.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.approval.domain.ApprovalRequest;
import com.procurax.approval.domain.ApprovalStep;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovalResponse(
        UUID id,
        UUID requesterUserId,
        UUID rfqId,
        UUID quotationId,
        String subject,
        String category,
        BigDecimal amount,
        String currency,
        int quotationCount,
        String justification,
        UUID policyEvaluationId,
        JsonNode policyDecisions,
        String status,
        List<ApprovalStepResponse> steps,
        Instant createdAt,
        long version) {

    public static ApprovalResponse from(ApprovalRequest request, List<ApprovalStep> steps) {
        return new ApprovalResponse(request.getId(), request.getRequesterUserId(), request.getRfqId(),
                request.getQuotationId(), request.getSubject(),
                request.getCategory(), request.getAmount(), request.getCurrency(),
                request.getQuotationCount(), request.getJustification(), request.getPolicyEvaluationId(),
                request.getPolicyDecisions(), request.getStatus().name(),
                steps.stream().map(ApprovalStepResponse::from).toList(),
                request.getCreatedAt(), request.getVersion());
    }
}
