package com.procurax.approval.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(
        @NotNull ApprovalDecision decision,
        @Size(max = 2000) String comment) {
}
