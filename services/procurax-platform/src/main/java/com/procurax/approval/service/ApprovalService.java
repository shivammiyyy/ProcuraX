package com.procurax.approval.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurax.approval.domain.ApprovalRequest;
import com.procurax.approval.domain.ApprovalStatus;
import com.procurax.approval.domain.ApprovalStep;
import com.procurax.approval.repository.ApprovalRequestRepository;
import com.procurax.approval.repository.ApprovalStepRepository;
import com.procurax.approval.web.ApprovalDecision;
import com.procurax.approval.web.ApprovalDecisionRequest;
import com.procurax.approval.web.ApprovalResponse;
import com.procurax.approval.web.ApprovalStepResponse;
import com.procurax.approval.web.CreateApprovalRequest;
import com.procurax.common.error.BusinessException;
import com.procurax.identity.domain.OrganizationMember;
import com.procurax.identity.domain.Role;
import com.procurax.identity.repository.OrganizationMemberRepository;
import com.procurax.identity.repository.RoleRepository;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.policy.service.PolicyService;
import com.procurax.policy.web.PolicyEvaluationResponse;
import com.procurax.policy.web.PurchasePolicyEvaluationRequest;
import com.procurax.quotation.domain.Quotation;
import com.procurax.quotation.repository.QuotationRepository;
import com.procurax.rfq.domain.Rfq;
import com.procurax.rfq.service.RfqService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final OrganizationMemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final OrganizationContext organizationContext;
    private final PolicyService policyService;
    private final QuotationRepository quotationRepository;
    private final RfqService rfqService;
    private final OutboxEventWriter eventWriter;
    private final ObjectMapper objectMapper;

    public ApprovalService(ApprovalRequestRepository requestRepository,
                           ApprovalStepRepository stepRepository,
                           OrganizationMemberRepository memberRepository,
                           RoleRepository roleRepository,
                           OrganizationContext organizationContext,
                           PolicyService policyService,
                           QuotationRepository quotationRepository,
                           RfqService rfqService,
                           OutboxEventWriter eventWriter,
                           ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.stepRepository = stepRepository;
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.organizationContext = organizationContext;
        this.policyService = policyService;
        this.quotationRepository = quotationRepository;
        this.rfqService = rfqService;
        this.eventWriter = eventWriter;
        this.objectMapper = objectMapper;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public ApprovalResponse create(CreateApprovalRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        validateApprover(organizationId, request.approverUserId());
        if (request.approverUserId().equals(principal.getUserId())) {
            throw badRequest("SELF_APPROVAL_NOT_ALLOWED",
                    "The requester cannot be assigned as their own approver");
        }

        Quotation quotation = quotationRepository.findByIdAndOrganizationId(
                        request.quotationId(), organizationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "QUOTATION_NOT_FOUND", "Quotation not found"));
        if (!"ACCEPTED".equals(quotation.getStatus())) {
            throw conflict("QUOTATION_NOT_ACCEPTED",
                    "Approval requests for purchase orders require an accepted quotation");
        }
        Rfq rfq = rfqService.requireRfq(quotation.getRfqId());
        if (!"IN_PROGRESS".equals(rfq.getStatus())) {
            throw conflict("RFQ_NOT_AWARDABLE", "The quotation's RFQ is not in an awardable state");
        }
        int quotationCount = Math.toIntExact(
                quotationRepository.countByOrganizationIdAndRfqId(organizationId, rfq.getId()));
        if (!rfq.getCategory().equalsIgnoreCase(request.category().trim())
                || quotation.getTotalAmount().compareTo(request.amount()) != 0
                || !quotation.getCurrency().equals(request.currency())
                || quotationCount != request.quotationCount()) {
            throw badRequest("PURCHASING_BASIS_MISMATCH",
                    "Category, amount, currency and quote count must match the accepted quotation and RFQ");
        }

        PolicyEvaluationResponse evaluation = policyService.evaluate(new PurchasePolicyEvaluationRequest(
                rfq.getCategory(), quotation.getTotalAmount(), quotation.getCurrency(), quotationCount));
        if ("BLOCKED".equals(evaluation.outcome())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "PURCHASE_BLOCKED_BY_POLICY",
                    "The proposed purchase is blocked by policy and cannot be submitted for approval");
        }
        if (!evaluation.requiresApproval()) {
            throw new BusinessException(HttpStatus.CONFLICT, "APPROVAL_NOT_REQUIRED",
                    "The current policy evaluation does not require human approval");
        }

        ApprovalRequest approval = new ApprovalRequest(organizationId, principal.getUserId(),
                rfq.getId(), quotation.getId(), request.subject().trim(), rfq.getCategory(),
                quotation.getTotalAmount(), quotation.getCurrency(), quotationCount, request.justification().trim(),
                evaluation.evaluationId(), objectMapper.valueToTree(evaluation.decisions()));
        approval.setCreatedBy(principal.getUserId());
        ApprovalRequest saved = requestRepository.saveAndFlush(approval);
        ApprovalStep step = new ApprovalStep(organizationId, saved.getId(), 1, request.approverUserId());
        step.setCreatedBy(principal.getUserId());
        ApprovalStep savedStep = stepRepository.save(step);

        Map<String, Object> payload = new HashMap<>();
        payload.put("approvalRequestId", saved.getId());
        payload.put("requesterUserId", principal.getUserId());
        payload.put("approverUserId", request.approverUserId());
        payload.put("rfqId", saved.getRfqId());
        payload.put("quotationId", saved.getQuotationId());
        payload.put("subject", saved.getSubject());
        payload.put("amount", saved.getAmount());
        payload.put("currency", saved.getCurrency());
        payload.put("policyEvaluationId", saved.getPolicyEvaluationId());
        payload.put("status", saved.getStatus());
        eventWriter.record("APPROVAL_REQUEST", saved.getId(), organizationId,
                "APPROVAL_REQUESTED", "procurax.approval.v1",
                OutboxEventWriter.currentCorrelationId(saved.getId()), principal.getUserId(), payload);
        stepRepository.flush();
        return ApprovalResponse.from(saved, List.of(savedStep));
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> list() {
        UUID organizationId = organizationContext.currentOrganizationId();
        return requestRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(request -> ApprovalResponse.from(request,
                        stepRepository.findAllByOrganizationIdAndRequestIdOrderByStepOrder(
                                organizationId, request.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalResponse get(UUID requestId) {
        UUID organizationId = organizationContext.currentOrganizationId();
        ApprovalRequest request = requestRepository.findByOrganizationIdAndId(organizationId, requestId)
                .orElseThrow(this::notFound);
        return ApprovalResponse.from(request,
                stepRepository.findAllByOrganizationIdAndRequestIdOrderByStepOrder(
                        organizationId, requestId));
    }

    @Transactional
    public ApprovalResponse decide(UUID requestId, ApprovalDecisionRequest decisionRequest) {
        UUID organizationId = organizationContext.currentOrganizationId();
        SecurityPrincipal principal = organizationContext.currentPrincipal();
        validateApprover(organizationId, principal.getUserId());

        ApprovalRequest request = requestRepository.findForUpdate(organizationId, requestId)
                .orElseThrow(this::notFound);
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw conflict("APPROVAL_ALREADY_DECIDED", "This approval request is no longer pending");
        }
        if (request.getRequesterUserId().equals(principal.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "SELF_APPROVAL_NOT_ALLOWED",
                    "The requester cannot approve their own purchase");
        }

        ApprovalStep step = stepRepository.findFirstStepForUpdate(organizationId, requestId)
                .orElseThrow(this::notFound);
        if (!step.getApproverUserId().equals(principal.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "APPROVAL_NOT_ASSIGNED",
                    "This approval request is assigned to another approver");
        }
        if (step.getStatus() != ApprovalStatus.PENDING) {
            throw conflict("APPROVAL_STEP_ALREADY_DECIDED", "This approval step is no longer pending");
        }

        String comment = decisionRequest.comment() == null ? null : decisionRequest.comment().trim();
        if (decisionRequest.decision() == ApprovalDecision.REJECT
                && (comment == null || comment.isBlank())) {
            throw badRequest("REJECTION_COMMENT_REQUIRED", "A rejection decision requires a comment");
        }
        ApprovalStatus status = decisionRequest.decision() == ApprovalDecision.APPROVE
                ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED;
        step.decide(status, comment == null || comment.isBlank() ? null : comment);
        step.setUpdatedBy(principal.getUserId());
        request.decide(status);
        request.setUpdatedBy(principal.getUserId());
        stepRepository.save(step);
        requestRepository.save(request);

        Map<String, Object> payload = new HashMap<>();
        payload.put("approvalRequestId", request.getId());
        payload.put("stepId", step.getId());
        payload.put("approverUserId", principal.getUserId());
        payload.put("decision", status);
        payload.put("comment", step.getDecisionComment());
        payload.put("decidedAt", step.getDecidedAt());
        eventWriter.record("APPROVAL_REQUEST", request.getId(), organizationId,
                status == ApprovalStatus.APPROVED ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED",
                "procurax.approval.v1", OutboxEventWriter.currentCorrelationId(request.getId()),
                principal.getUserId(), payload);
        stepRepository.flush();
        requestRepository.flush();
        return ApprovalResponse.from(request, List.of(step));
    }

    private void validateApprover(UUID organizationId, UUID userId) {
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
                .orElseThrow(() -> badRequest("INVALID_APPROVER",
                        "The selected approver must be an active member of this organization"));
        Role role = roleRepository.findById(member.getRoleId())
                .orElseThrow(() -> badRequest("INVALID_APPROVER", "The selected member has no approver role"));
        if (!"APPROVER".equals(role.getName()) && !"ORG_ADMIN".equals(role.getName())) {
            throw badRequest("INVALID_APPROVER",
                    "The selected organization member does not have an approver role");
        }
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "APPROVAL_NOT_FOUND",
                "Approval request not found");
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }
}
