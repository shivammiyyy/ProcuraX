package com.procurax.approval.domain;

import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_steps")
public class ApprovalStep extends BaseEntity {

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "step_order", nullable = false, updatable = false)
    private int stepOrder;

    @Column(name = "approver_user_id", nullable = false, updatable = false)
    private UUID approverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "decision_comment", length = 2000)
    private String decisionComment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ApprovalStep() {
        super();
    }

    public ApprovalStep(UUID organizationId, UUID requestId, int stepOrder, UUID approverUserId) {
        super(organizationId);
        this.requestId = requestId;
        this.stepOrder = stepOrder;
        this.approverUserId = approverUserId;
    }

    public void decide(ApprovalStatus decision, String comment) {
        if (decision == ApprovalStatus.PENDING) {
            throw new IllegalArgumentException("An approval decision must be final");
        }
        this.status = decision;
        this.decisionComment = comment;
        this.decidedAt = Instant.now();
    }

    public UUID getRequestId() { return requestId; }
    public int getStepOrder() { return stepOrder; }
    public UUID getApproverUserId() { return approverUserId; }
    public ApprovalStatus getStatus() { return status; }
    public String getDecisionComment() { return decisionComment; }
    public Instant getDecidedAt() { return decidedAt; }
}
