package com.procurax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FoundationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    private List<String> permissionsOf(String role) {
        return jdbc.queryForList("""
                SELECT p.name FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE r.name = ?""", String.class, role);
    }

    @Test
    void migrationsSeedRolesAndPermissions() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM roles", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM permissions", Integer.class)).isEqualTo(25);
        assertThat(permissionsOf("FINANCE")).contains("PAYMENT_CAPTURE", "PAYMENT_AUTHORIZE");
        assertThat(permissionsOf("BUYER")).doesNotContain("PO_APPROVE", "PAYMENT_CAPTURE");
        assertThat(permissionsOf("VENDOR")).containsExactlyInAnyOrder(
                "RFQ_READ", "QUOTE_READ", "QUOTE_SUBMIT", "CONTRACT_READ");
        assertThat(permissionsOf("VIEWER")).allMatch(p -> p.endsWith("_READ")).doesNotContain("AUDIT_READ");
    }

    @Test
    void processedEventsRejectDuplicates() {
        UUID eventId = UUID.randomUUID();
        String sql = "INSERT INTO processed_events (consumer, event_id) VALUES ('payments', ?) ON CONFLICT DO NOTHING";
        assertThat(jdbc.update(sql, eventId)).isEqualTo(1);
        assertThat(jdbc.update(sql, eventId)).isZero();
    }

    @Test
    void errorsUseStandardFormatAndEchoValidCorrelationId() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get("/api/v1/does-not-exist").header("X-Correlation-ID", id))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", id))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.correlationId").value(id))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
