package com.procurax.approval.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateApprovalRequest(
        @NotBlank @Size(max = 160) String subject,
        @NotBlank @Size(max = 100) String category,
        @NotNull UUID quotationId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @Min(0) @Max(100) int quotationCount,
        @NotBlank @Size(max = 2000) String justification,
        @NotNull UUID approverUserId) {
}
