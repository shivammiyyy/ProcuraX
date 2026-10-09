package com.procurax.contract.web;

import java.time.Instant;

public record ContractDocumentDownloadResponse(String downloadUrl, Instant expiresAt) {
}
