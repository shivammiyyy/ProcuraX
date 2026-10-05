package com.procurax.contract.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContractDecisionRequest(
        @NotNull Decision decision,
        @Size(max = 10000) String comment) {

    public enum Decision {
        APPROVE, REJECT
    }
}
