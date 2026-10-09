package com.procurax.policy.web;

import com.procurax.policy.domain.PolicyRule;
import com.procurax.policy.domain.PolicyRuleType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PolicyRuleResponse(
        UUID id,
        String name,
        String category,
        PolicyRuleType ruleType,
        BigDecimal thresholdAmount,
        String thresholdCurrency,
        List<String> allowedCurrencyCodes,
        Integer minimumQuoteCount,
        boolean enabled,
        long version) {

    public static PolicyRuleResponse from(PolicyRule rule) {
        List<String> currencies = rule.getAllowedCurrencyCodes() == null
                ? List.of()
                : java.util.stream.StreamSupport.stream(
                        rule.getAllowedCurrencyCodes().spliterator(), false)
                        .map(com.fasterxml.jackson.databind.JsonNode::asText)
                        .toList();
        return new PolicyRuleResponse(rule.getId(), rule.getName(), rule.getCategory(), rule.getRuleType(),
                rule.getThresholdAmount(), rule.getThresholdCurrency(), currencies, rule.getMinimumQuoteCount(),
                rule.isEnabled(), rule.getVersion());
    }
}
