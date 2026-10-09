package com.procurax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.procurax.identity.domain.Organization;
import com.procurax.identity.repository.OrganizationRepository;
import com.procurax.identity.security.SecurityPrincipal;
import com.procurax.outbox.OutboxReplayService;
import java.util.Map;
import java.util.Set;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    ProcessedEventStore processedEventStore;

    @Autowired
    OutboxEventWriter outboxEventWriter;

    @Autowired
    OutboxReplayService outboxReplayService;

    @Autowired
    OrganizationRepository organizationRepository;

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
        assertThat(permissionsOf("PLATFORM_ADMIN")).contains("OUTBOX_REPLAY");
        assertThat(permissionsOf("ORG_ADMIN")).doesNotContain("OUTBOX_REPLAY");
        assertThat(permissionsOf("VIEWER")).allMatch(p -> p.endsWith("_READ")).doesNotContain("AUDIT_READ");
    }

    @Test
    void deadLetterReplayRequiresPlatformPermissionIsTenantScopedAuditedAndCapped() throws Exception {
        Organization organization = organizationRepository.save(new Organization(
                "Replay Org", "replay-" + UUID.randomUUID()));
        UUID actorId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO outbox_events
                    (id, organization_id, aggregate_type, aggregate_id, event_type, topic,
                     correlation_id, payload, status, retry_count, dead_lettered_at)
                VALUES (?, ?, 'TEST', ?, 'TEST_EVENT', 'procurax.test.v1',
                        ?, '{}'::jsonb, 'DEAD_LETTERED', 8, now())
                """, eventId, organization.getId(), aggregateId, UUID.randomUUID());

        mvc.perform(post("/api/v1/operations/outbox/{eventId}/replay", eventId)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("org-admin")
                                .authorities(() -> "AUDIT_READ"))
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Retry after confirming the broker recovered."}
                                """))
                .andExpect(status().isForbidden());

        SecurityPrincipal principal = new SecurityPrincipal(actorId, "platform@example.test",
                "Platform Operator", organization.getId(), organization.getName(), "PLATFORM_ADMIN",
                Set.of("OUTBOX_REPLAY"), Map.of());
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal, null, principal.getAuthorities());
        try {
            mvc.perform(post("/api/v1/operations/outbox/{eventId}/replay", eventId)
                            .with(org.springframework.security.test.web.servlet.request
                                    .SecurityMockMvcRequestPostProcessors.authentication(authentication))
                            .with(org.springframework.security.test.web.servlet.request
                                    .SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reason":"Retry after confirming the broker recovered."}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.replayCount").value(1));
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(jdbc.queryForObject("""
                SELECT status || ':' || retry_count || ':' || replay_count
                FROM outbox_events WHERE id = ?
                """, String.class, eventId)).isEqualTo("PENDING:0:1");
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                WHERE organization_id = ? AND resource_id = ?
                  AND actor_id = ? AND action = 'OUTBOX_EVENT_REPLAY_REQUESTED'
                  AND details->>'reason' = ?
                """, Integer.class, organization.getId(), eventId.toString(), actorId.toString(),
                "Retry after confirming the broker recovered.")).isEqualTo(1);

        Organization foreignOrganization = organizationRepository.save(new Organization(
                "Foreign Replay Org", "replay-foreign-" + UUID.randomUUID()));
        UUID foreignEventId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO outbox_events
                    (id, organization_id, aggregate_type, aggregate_id, event_type, topic,
                     correlation_id, payload, status, retry_count, dead_lettered_at)
                VALUES (?, ?, 'TEST', ?, 'FOREIGN_EVENT', 'procurax.test.v1',
                        ?, '{}'::jsonb, 'DEAD_LETTERED', 8, now())
                """, foreignEventId, foreignOrganization.getId(), UUID.randomUUID(), UUID.randomUUID());
        mvc.perform(post("/api/v1/operations/outbox/{eventId}/replay", foreignEventId)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Attempt cross-tenant replay."}
                                """))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events WHERE id = ?",
                String.class, foreignEventId)).isEqualTo("DEAD_LETTERED");

        jdbc.update("UPDATE outbox_events SET status = 'DEAD_LETTERED', dead_lettered_at = now(), replay_count = 3 WHERE id = ?",
                eventId);
        mvc.perform(post("/api/v1/operations/outbox/{eventId}/replay", eventId)
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.authentication(authentication))
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Limit replay for safety."}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUTBOX_REPLAY_LIMIT_REACHED"));
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
