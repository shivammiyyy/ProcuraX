package com.procurax.policy.web;

import com.procurax.policy.domain.PolicyRuleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record PolicyRuleRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 100) String category,
        @NotNull PolicyRuleType ruleType,
        @DecimalMin(value = "0.01") @Digits(integer = 12, fraction = 2) BigDecimal thresholdAmount,
        @Pattern(regexp = "[A-Z]{3}") String thresholdCurrency,
        @Size(min = 1, max = 20)
        List<@Pattern(regexp = "[A-Z]{3}") String> allowedCurrencyCodes,
        @Min(2) @Max(20) Integer minimumQuoteCount,
        @NotNull Boolean enabled) {
}
