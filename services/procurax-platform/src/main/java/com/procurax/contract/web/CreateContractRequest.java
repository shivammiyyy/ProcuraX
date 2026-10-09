package com.procurax.contract.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateContractRequest(
        @NotNull UUID quotationId,
        @NotBlank @Size(max = 100000) String content,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
