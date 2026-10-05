package com.procurax.contract.web;

import com.procurax.contract.domain.Contract;
import java.util.UUID;

public record ContractDocumentResponse(
        UUID contractId,
        String fileName) {

    public static ContractDocumentResponse from(Contract contract) {
        return new ContractDocumentResponse(contract.getId(), contract.getDocumentFileName());
    }
}
