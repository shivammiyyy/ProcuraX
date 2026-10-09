package com.procurax.quotation.web;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public record UpdateQuotationStatusRequest(
        @NotBlank @Pattern(regexp = "UNDER_REVIEW|ACCEPTED|REJECTED") String status) {
}
