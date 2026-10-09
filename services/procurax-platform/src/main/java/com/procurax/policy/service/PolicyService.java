package com.procurax.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurax.common.correlation.CorrelationIdFilter;
import com.procurax.common.error.BusinessException;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.outbox.OutboxEventWriter;
import com.procurax.policy.domain.PolicyRule;
import com.procurax.policy.domain.PolicyRuleType;
import com.procurax.policy.repository.PolicyRuleRepository;
import com.procurax.policy.web.PolicyEvaluationResponse;
import com.procurax.policy.web.PolicyRuleRequest;
import com.procurax.policy.web.PolicyRuleResponse;
import com.procurax.policy.web.PurchasePolicyEvaluationRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyService {

    private final PolicyRuleRepository ruleRepository;
    private final OrganizationContext organizationContext;
    private final OutboxEventWriter eventWriter;
    private final ObjectMapper objectMapper;

    public PolicyService(PolicyRuleRepository ruleRepository, OrganizationContext organizationContext,
                         OutboxEventWriter eventWriter, ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.organizationContext = organizationContext;
        this.eventWriter = eventWriter;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PolicyRuleResponse> listRules() {
        UUID organizationId = organizationContext.currentOrganizationId();
        return ruleRepository.findAllByOrganizationIdOrderByName(organizationId)
                .stream().map(PolicyRuleResponse::from).toList();
    }

    @Transactional
    public PolicyRuleResponse createRule(PolicyRuleRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        String name = normalizedName(request.name());
        validateConfiguration(request);
        if (ruleRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, name)) {
            throw conflict("POLICY_RULE_NAME_EXISTS", "A policy rule with this name already exists");
        }

        PolicyRule rule = new PolicyRule(organizationId, name, normalizedCategory(request.category()),
                request.ruleType(), request.thresholdAmount(), request.thresholdCurrency(),
                currencyJson(request.allowedCurrencyCodes()),
                request.minimumQuoteCount(), request.enabled());
        rule.setCreatedBy(organizationContext.currentPrincipal().getUserId());
        PolicyRule saved = ruleRepository.save(rule);
        recordRuleEvent(saved, "POLICY_RULE_CREATED");
        return PolicyRuleResponse.from(saved);
    }

    @Transactional
    public PolicyRuleResponse updateRule(UUID ruleId, PolicyRuleRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        String name = normalizedName(request.name());
        validateConfiguration(request);
        PolicyRule rule = ruleRepository.findByOrganizationIdAndId(organizationId, ruleId)
                .orElseThrow(this::notFound);
        if (ruleRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                organizationId, name, ruleId)) {
            throw conflict("POLICY_RULE_NAME_EXISTS", "A policy rule with this name already exists");
        }

        rule.update(name, normalizedCategory(request.category()), request.ruleType(),
                request.thresholdAmount(), request.thresholdCurrency(),
                currencyJson(request.allowedCurrencyCodes()), request.minimumQuoteCount(),
                request.enabled());
        rule.setUpdatedBy(organizationContext.currentPrincipal().getUserId());
        PolicyRule saved = ruleRepository.save(rule);
        recordRuleEvent(saved, "POLICY_RULE_UPDATED");
        return PolicyRuleResponse.from(saved);
    }

    @Transactional
    public PolicyEvaluationResponse evaluate(PurchasePolicyEvaluationRequest request) {
        UUID organizationId = organizationContext.currentOrganizationId();
        UUID actorId = organizationContext.currentPrincipal().getUserId();
        UUID evaluationId = UUID.randomUUID();
        List<PolicyRule> activeRules =
                ruleRepository.findAllByOrganizationIdAndEnabledTrueOrderByName(organizationId);
        List<PolicyEvaluationResponse.PolicyDecision> decisions = new ArrayList<>();

        int applicableRuleCount = 0;
        for (PolicyRule rule : activeRules) {
            if (rule.getCategory() != null
                    && !rule.getCategory().equalsIgnoreCase(request.category().trim())) {
                continue;
            }
            applicableRuleCount++;
            evaluateRule(rule, request, decisions);
        }
        if (applicableRuleCount == 0) {
            decisions.add(new PolicyEvaluationResponse.PolicyDecision(
                    null, null, "APPROVAL_REQUIRED", activeRules.isEmpty()
                            ? "No active purchasing policies are configured; request an authorized review."
                            : "No active purchasing policies apply to this category; request an authorized review."));
        }

        boolean blocked = decisions.stream().anyMatch(decision -> "BLOCKED".equals(decision.outcome()));
        boolean requiresApproval =
                decisions.stream().anyMatch(decision -> "APPROVAL_REQUIRED".equals(decision.outcome()));
        String outcome = blocked ? "BLOCKED" : requiresApproval ? "APPROVAL_REQUIRED" : "POLICY_PASSED";
        PolicyEvaluationResponse response = new PolicyEvaluationResponse(
                evaluationId, outcome, requiresApproval, !blocked && !requiresApproval, decisions);

        eventWriter.record(
                "POLICY_EVALUATION",
                evaluationId,
                organizationId,
                "POLICY_EVALUATED",
                "procurax.policy.v1",
                currentCorrelationId(evaluationId),
                actorId,
                Map.of(
                        "outcome", outcome,
                        "amount", request.amount(),
                        "currency", request.currency(),
                        "category", request.category(),
                        "quotationCount", request.quotationCount(),
                        "decisionCount", decisions.size()));
        return response;
    }

    private void evaluateRule(PolicyRule rule, PurchasePolicyEvaluationRequest request,
                              List<PolicyEvaluationResponse.PolicyDecision> decisions) {
        switch (rule.getRuleType()) {
            case MAX_PURCHASE_AMOUNT -> {
                if (!rule.getThresholdCurrency().equals(request.currency())) {
                    decisions.add(decision(rule, "APPROVAL_REQUIRED",
                            "Manual review is required because currency conversion is unavailable."));
                } else if (request.amount().compareTo(rule.getThresholdAmount()) > 0) {
                    decisions.add(decision(rule, "BLOCKED",
                            "Amount exceeds the configured maximum purchase amount."));
                } else {
                    decisions.add(decision(rule, "PASSED", "Amount is within the configured limit."));
                }
            }
            case APPROVAL_THRESHOLD -> {
                if (!rule.getThresholdCurrency().equals(request.currency())) {
                    decisions.add(decision(rule, "APPROVAL_REQUIRED",
                            "Manual review is required because currency conversion is unavailable."));
                } else if (request.amount().compareTo(rule.getThresholdAmount()) >= 0) {
                    decisions.add(decision(rule, "APPROVAL_REQUIRED",
                            "Amount meets or exceeds the configured approval threshold."));
                } else {
                    decisions.add(decision(rule, "PASSED", "Amount is below the approval threshold."));
                }
            }
            case ALLOWED_CURRENCIES -> {
                Set<String> allowed = new HashSet<>();
                rule.getAllowedCurrencyCodes().forEach(currency -> allowed.add(currency.asText()));
                if (!allowed.contains(request.currency())) {
                    decisions.add(decision(rule, "BLOCKED",
                            "Currency is not included in the organization's allowed currency list."));
                } else {
                    decisions.add(decision(rule, "PASSED", "Currency is allowed."));
                }
            }
            case MINIMUM_QUOTE_COUNT -> {
                if (request.quotationCount() < rule.getMinimumQuoteCount()) {
                    decisions.add(decision(rule, "APPROVAL_REQUIRED",
                            "The quote count is below the configured minimum and requires review."));
                } else {
                    decisions.add(decision(rule, "PASSED", "The quote count meets the configured minimum."));
                }
            }
        }
    }

    private PolicyEvaluationResponse.PolicyDecision decision(
            PolicyRule rule, String outcome, String reason) {
        return new PolicyEvaluationResponse.PolicyDecision(
                rule.getId(), rule.getName(), outcome, reason);
    }

    private void validateConfiguration(PolicyRuleRequest request) {
        boolean valid = switch (request.ruleType()) {
            case MAX_PURCHASE_AMOUNT, APPROVAL_THRESHOLD ->
                    request.thresholdAmount() != null
                            && request.thresholdAmount().compareTo(BigDecimal.ZERO) > 0
                            && request.thresholdCurrency() != null
                            && request.allowedCurrencyCodes() == null
                            && request.minimumQuoteCount() == null;
            case ALLOWED_CURRENCIES ->
                    request.thresholdAmount() == null
                            && request.thresholdCurrency() == null
                            && request.allowedCurrencyCodes() != null
                            && !request.allowedCurrencyCodes().isEmpty()
                            && request.minimumQuoteCount() == null
                            && request.allowedCurrencyCodes().stream().distinct().count()
                                    == request.allowedCurrencyCodes().size();
            case MINIMUM_QUOTE_COUNT ->
                    request.thresholdAmount() == null
                            && request.thresholdCurrency() == null
                            && request.allowedCurrencyCodes() == null
                            && request.minimumQuoteCount() != null
                            && request.minimumQuoteCount() >= 2
                            && request.minimumQuoteCount() <= 20;
        };
        if (!valid) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_POLICY_CONFIGURATION",
                    "Rule configuration must include only the fields required by its type");
        }
    }

    private JsonNode currencyJson(List<String> currencies) {
        return currencies == null ? null : objectMapper.valueToTree(currencies);
    }

    private void recordRuleEvent(PolicyRule rule, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ruleId", rule.getId());
        payload.put("name", rule.getName());
        payload.put("category", rule.getCategory());
        payload.put("ruleType", rule.getRuleType());
        payload.put("thresholdAmount", rule.getThresholdAmount());
        payload.put("thresholdCurrency", rule.getThresholdCurrency());
        payload.put("allowedCurrencyCodes", rule.getAllowedCurrencyCodes());
        payload.put("minimumQuoteCount", rule.getMinimumQuoteCount());
        payload.put("enabled", rule.isEnabled());
        eventWriter.record("POLICY_RULE", rule.getId(), rule.getOrganizationId(),
                eventType, "procurax.policy.v1", currentCorrelationId(rule.getId()),
                organizationContext.currentPrincipal().getUserId(), payload);
    }

    private UUID currentCorrelationId(UUID fallback) {
        String correlationId = CorrelationIdFilter.current();
        return correlationId == null ? fallback : UUID.fromString(correlationId);
    }

    private String normalizedName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String normalizedCategory(String category) {
        return category == null || category.isBlank() ? null : category.trim();
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "POLICY_RULE_NOT_FOUND",
                "Policy rule not found");
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
