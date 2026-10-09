package com.procurax.policy.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "policy_rules")
public class PolicyRule extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private PolicyRuleType ruleType;

    @Column(name = "threshold_amount", precision = 14, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "threshold_currency", length = 3)
    private String thresholdCurrency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_currency_codes", columnDefinition = "jsonb")
    private JsonNode allowedCurrencyCodes;

    @Column(name = "minimum_quote_count")
    private Integer minimumQuoteCount;

    @Column(nullable = false)
    private boolean enabled;

    protected PolicyRule() {
        super();
    }

    public PolicyRule(UUID organizationId, String name, String category, PolicyRuleType ruleType,
                      BigDecimal thresholdAmount, String thresholdCurrency, JsonNode allowedCurrencyCodes,
                      Integer minimumQuoteCount, boolean enabled) {
        super(organizationId);
        update(name, category, ruleType, thresholdAmount, thresholdCurrency,
                allowedCurrencyCodes, minimumQuoteCount, enabled);
    }

    public void update(String name, String category, PolicyRuleType ruleType, BigDecimal thresholdAmount,
                       String thresholdCurrency, JsonNode allowedCurrencyCodes,
                       Integer minimumQuoteCount, boolean enabled) {
        this.name = name;
        this.category = category;
        this.ruleType = ruleType;
        this.thresholdAmount = thresholdAmount;
        this.thresholdCurrency = thresholdCurrency;
        this.allowedCurrencyCodes = allowedCurrencyCodes;
        this.minimumQuoteCount = minimumQuoteCount;
        this.enabled = enabled;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public PolicyRuleType getRuleType() { return ruleType; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public String getThresholdCurrency() { return thresholdCurrency; }
    public JsonNode getAllowedCurrencyCodes() { return allowedCurrencyCodes; }
    public Integer getMinimumQuoteCount() { return minimumQuoteCount; }
    public boolean isEnabled() { return enabled; }
}
