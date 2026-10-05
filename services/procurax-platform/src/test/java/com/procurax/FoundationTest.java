package com.procurax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import com.procurax.outbox.ProcessedEventStore;
import com.procurax.outbox.OutboxEventWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class FoundationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    ProcessedEventStore processedEventStore;

    @Autowired
    OutboxEventWriter outboxEventWriter;

    @Autowired
    PlatformTransactionManager transactionManager;

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
        assertThat(jdbc.queryForObject("SELECT count(*) FROM permissions", Integer.class)).isGreaterThanOrEqualTo(28);
        assertThat(permissionsOf("BUYER")).contains("EVENT_STREAM_READ");
        assertThat(permissionsOf("VENDOR")).doesNotContain("EVENT_STREAM_READ");
        assertThat(permissionsOf("FINANCE")).contains("PAYMENT_CAPTURE", "PAYMENT_AUTHORIZE");
        assertThat(permissionsOf("BUYER")).doesNotContain("PO_APPROVE", "PAYMENT_CAPTURE");
        assertThat(permissionsOf("VENDOR")).containsExactlyInAnyOrder(
                "RFQ_READ", "QUOTE_READ", "QUOTE_SUBMIT", "CONTRACT_READ", "VENDOR_DOCUMENT_SUBMIT");
        assertThat(permissionsOf("ORG_ADMIN")).contains("VENDOR_DOCUMENT_VERIFY");
        assertThat(permissionsOf("VIEWER")).allMatch(p -> p.endsWith("_READ")).doesNotContain("AUDIT_READ");
    }

    @Test
    void processedEventsRejectDuplicates() {
        UUID eventId = UUID.randomUUID();
        assertThat(processedEventStore.claim("payments", eventId)).isTrue();
        assertThat(processedEventStore.claim("payments", eventId)).isFalse();
    }

    @Test
    void outboxHasRetryAndDeadLetterDeliveryState() {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name = 'outbox_events'
                  AND column_name IN ('next_attempt_at', 'last_attempt_at', 'last_error', 'dead_lettered_at')
                """, Integer.class)).isEqualTo(4);
    }

    @Test
    void outboxAndAuditWritesRollBackWithTheirBusinessTransaction() {
                UUID aggregateId = UUID.randomUUID();
                UUID organizationId = UUID.randomUUID();
                UUID actorId = UUID.randomUUID();
                UUID correlationId = UUID.randomUUID();
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);

                org.assertj.core.api.Assertions.assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    outboxEventWriter.record("TEST", aggregateId, organizationId, "TEST_EVENT",
                            "procurax.test.v1", correlationId, actorId, java.util.Map.of("value", 1));
                    throw new IllegalStateException("rollback test");
                })).isInstanceOf(IllegalStateException.class);

                assertThat(jdbc.queryForObject("""
                        SELECT count(*) FROM outbox_events WHERE aggregate_id = ?
                        """, Integer.class, aggregateId)).isZero();
                assertThat(jdbc.queryForObject("""
                        SELECT count(*) FROM audit_events WHERE resource_id = ?
                        """, Integer.class, aggregateId.toString())).isZero();
    }

    @Test
    void pendingOutboxEventsAreClaimedInOrderPerAggregate() {
                        UUID aggregateId = UUID.randomUUID();
                        UUID first = UUID.randomUUID();
                        UUID second = UUID.randomUUID();
                        jdbc.update("""
                                INSERT INTO outbox_events
                                    (id, aggregate_type, aggregate_id, event_type, topic, correlation_id, payload, created_at)
                                VALUES (?, 'ORDER_TEST', ?, 'FIRST', 'procurax.test.v1', ?, '{}'::jsonb, now() - interval '1 second'),
                                       (?, 'ORDER_TEST', ?, 'SECOND', 'procurax.test.v1', ?, '{}'::jsonb, now())
                                """, first, aggregateId, UUID.randomUUID(), second, aggregateId, UUID.randomUUID());

                        List<UUID> eligible = jdbc.query("""
                                SELECT event.id
                                FROM outbox_events event
                                WHERE event.status = 'PENDING' AND event.next_attempt_at <= now()
                                  AND NOT EXISTS (
                                      SELECT 1 FROM outbox_events earlier
                                      WHERE earlier.aggregate_type = event.aggregate_type
                                        AND earlier.aggregate_id = event.aggregate_id
                                        AND earlier.status = 'PENDING'
                                        AND (earlier.created_at, earlier.id) < (event.created_at, event.id)
                                  )
                                ORDER BY event.created_at, event.id
                                LIMIT 25
                                FOR UPDATE OF event SKIP LOCKED
                                """, (row, index) -> row.getObject(1, UUID.class));
                        assertThat(eligible).contains(first).doesNotContain(second);

                        jdbc.update("UPDATE outbox_events SET status = 'PUBLISHED', published_at = now() WHERE id = ?", first);
                        eligible = jdbc.query("""
                                SELECT event.id
                                FROM outbox_events event
                                WHERE event.id = ? AND event.status = 'PENDING' AND event.next_attempt_at <= now()
                                  AND NOT EXISTS (
                                      SELECT 1 FROM outbox_events earlier
                                      WHERE earlier.aggregate_type = event.aggregate_type
                                        AND earlier.aggregate_id = event.aggregate_id
                                        AND earlier.status = 'PENDING'
                                        AND (earlier.created_at, earlier.id) < (event.created_at, event.id)
                                  )
                                FOR UPDATE OF event SKIP LOCKED
                                """, (row, index) -> row.getObject(1, UUID.class), second);
                        assertThat(eligible).containsExactly(second);
    }

    @Test
    void errorsUseStandardFormatAndEchoValidCorrelationId() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get("/api/v1/does-not-exist").header("X-Correlation-ID", id).with(user("tester")))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", id))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.correlationId").value(id))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
