package com.procurax.common.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorrelationIdFilterTest {

    @Test
    void keepsValidUuid() {
        String id = UUID.randomUUID().toString();
        assertThat(CorrelationIdFilter.resolve(id)).isEqualTo(id);
    }

    @Test
    void replacesMissingOrMalformedValues() {
        assertThat(UUID.fromString(CorrelationIdFilter.resolve(null))).isNotNull();
        String malicious = "abc\r\nSet-Cookie: x=1";
        String resolved = CorrelationIdFilter.resolve(malicious);
        assertThat(resolved).isNotEqualTo(malicious);
        assertThat(UUID.fromString(resolved)).isNotNull();
    }
}
