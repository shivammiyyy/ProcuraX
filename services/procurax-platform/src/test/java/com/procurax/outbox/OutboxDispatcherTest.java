package com.procurax.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class OutboxDispatcherTest {

    private static final UUID EVENT_ID = UUID.fromString("873b3fd2-f8f2-4b43-80b4-a90927e0b2b0");
    private static final UUID ORGANIZATION_ID = UUID.fromString("9f4c0c18-3df8-451b-9541-5cb4ba40088b");
    private static final UUID AGGREGATE_ID = UUID.fromString("6238ce12-ea7d-4e83-9204-434669eab842");
    private static final Instant CREATED_AT = Instant.parse("2026-10-05T10:00:00Z");

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void configureDueEventQuery() throws Exception {
        when(resultSet.getObject("id", UUID.class)).thenReturn(EVENT_ID);
        when(resultSet.getObject("organization_id", UUID.class)).thenReturn(ORGANIZATION_ID);
        when(resultSet.getString("aggregate_type")).thenReturn("RFQ");
        when(resultSet.getObject("aggregate_id", UUID.class)).thenReturn(AGGREGATE_ID);
        when(resultSet.getString("event_type")).thenReturn("RFQ_PUBLISHED");
        when(resultSet.getString("topic")).thenReturn("procurax.rfq.v1");
        when(resultSet.getObject("correlation_id", UUID.class)).thenReturn(EVENT_ID);
        when(resultSet.getObject("causation_id", UUID.class)).thenReturn(null);
        when(resultSet.getString("payload")).thenReturn("{\"rfqId\":\"" + AGGREGATE_ID + "\"}");
        when(resultSet.getInt("retry_count")).thenReturn(0);
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(CREATED_AT));
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenAnswer(invocation -> {
            RowMapper<?> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(resultSet, 0));
        });
    }

    @Test
    void successfulPublishMarksOutboxEventPublishedAndWrapsEnvelope() throws Exception {
        when(kafka.send(eq("procurax.rfq.v1"), eq(AGGREGATE_ID.toString()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        OutboxDispatcher dispatcher = dispatcher(8);

        dispatcher.dispatch();

        ArgumentCaptor<String> envelope = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("procurax.rfq.v1"), eq(AGGREGATE_ID.toString()), envelope.capture());
        var body = objectMapper.readTree(envelope.getValue());
        assertThat(body.get("eventId").asText()).isEqualTo(EVENT_ID.toString());
        assertThat(body.get("organizationId").asText()).isEqualTo(ORGANIZATION_ID.toString());
        assertThat(body.get("eventType").asText()).isEqualTo("RFQ_PUBLISHED");
        assertThat(body.path("payload").path("rfqId").asText()).isEqualTo(AGGREGATE_ID.toString());
        verify(jdbc).update(org.mockito.ArgumentMatchers.contains("status = 'PUBLISHED'"),
                eq(EVENT_ID));
    }

    @Test
    void failedPublishSchedulesExponentialRetry() {
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafka.send(eq("procurax.rfq.v1"), eq(AGGREGATE_ID.toString()), anyString()))
                .thenReturn(failed);
        OutboxDispatcher dispatcher = dispatcher(8);

        dispatcher.dispatch();

        verify(jdbc).update(org.mockito.ArgumentMatchers.contains("retry_count = ?, last_attempt_at"),
                eq(1), eq("broker unavailable"), eq(15L), eq(EVENT_ID));
    }

    @Test
    void exhaustedEventIsSentToDeadLetterTopic() throws Exception {
        when(resultSet.getInt("retry_count")).thenReturn(8);
        when(kafka.send(eq("procurax.rfq.v1.DLT"), eq(AGGREGATE_ID.toString()), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        OutboxDispatcher dispatcher = dispatcher(8);

        dispatcher.dispatch();

        verify(kafka).send(eq("procurax.rfq.v1.DLT"), eq(AGGREGATE_ID.toString()), anyString());
        verify(jdbc).update(org.mockito.ArgumentMatchers.contains("status = 'DEAD_LETTERED'"),
                eq(EVENT_ID));
    }

    private OutboxDispatcher dispatcher(int maxAttempts) {
        return new OutboxDispatcher(jdbc, kafka, objectMapper, 25, maxAttempts, 15, 5, 1000);
    }
}
