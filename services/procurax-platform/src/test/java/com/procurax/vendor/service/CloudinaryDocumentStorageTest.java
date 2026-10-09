package com.procurax.vendor.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.procurax.common.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CloudinaryDocumentStorageTest {

    @Test
    void uploadFailsExplicitlyWhenCloudinaryCredentialsAreMissing() {
        CloudinaryDocumentStorage storage = new CloudinaryDocumentStorage("", "", "");

        assertThatThrownBy(() -> storage.upload(
                new byte[] {'%', 'P', 'D', 'F', '-'}, "policy.pdf", "raw",
                "procurement/vendor_documents", "upload"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getStatus())
                            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    org.assertj.core.api.Assertions.assertThat(exception.getCode())
                            .isEqualTo("DOCUMENT_STORAGE_UNAVAILABLE");
                });
    }

    @Test
    void downloadLinkGenerationFailsExplicitlyWhenCloudinaryCredentialsAreMissing() {
        CloudinaryDocumentStorage storage = new CloudinaryDocumentStorage("", "", "");

        assertThatThrownBy(() -> storage.createDownloadUrl(
                "procurement/contract_documents/agreement", "pdf", "raw", "authenticated",
                "agreement.pdf", java.time.Instant.now().plusSeconds(300)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    org.assertj.core.api.Assertions.assertThat(exception.getStatus())
                            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    org.assertj.core.api.Assertions.assertThat(exception.getCode())
                            .isEqualTo("DOCUMENT_STORAGE_UNAVAILABLE");
                });
    }
}
