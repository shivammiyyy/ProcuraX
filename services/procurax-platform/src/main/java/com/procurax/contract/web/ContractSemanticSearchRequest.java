package com.procurax.contract.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractSemanticSearchRequest(
        @NotBlank @Size(max = 1000) String query) {
}
