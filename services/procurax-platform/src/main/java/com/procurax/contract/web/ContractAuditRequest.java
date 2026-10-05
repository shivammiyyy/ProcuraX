package com.procurax.contract.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ContractAuditRequest(
        @NotNull RiskLevel riskLevel,
        @NotBlank @Size(max = 10000) String finding,
        @Size(max = 10000) String clause,
        @Size(max = 10000) String explanation,
        @Size(max = 10000) String recommendation,
        @DecimalMin("0.000") @DecimalMax("1.000") BigDecimal confidence) {

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
