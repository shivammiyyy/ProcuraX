package com.procurax.contract.web;

import com.procurax.contract.domain.Contract;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        UUID rfqId,
        UUID quotationId,
        UUID vendorId,
        String vendorName,
        String content,
        String documentFileName,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String auditStatus,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static ContractResponse from(Contract contract, String vendorName) {
        return new ContractResponse(contract.getId(), contract.getRfqId(), contract.getQuotationId(),
                contract.getVendorId(), vendorName, contract.getContent(), contract.getDocumentFileName(),
                contract.getStartDate(), contract.getEndDate(), contract.getStatus(),
                contract.getAuditStatus(), contract.getCreatedAt(), contract.getUpdatedAt(),
                contract.getVersion());
    }
}
