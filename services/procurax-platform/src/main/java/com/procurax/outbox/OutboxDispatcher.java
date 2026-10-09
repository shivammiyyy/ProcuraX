package com.procurax.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "procurax.outbox", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final int maxAttempts;
    private final long retryDelaySeconds;
    private final long sendTimeoutSeconds;

    public OutboxDispatcher(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka,
                            ObjectMapper objectMapper,
                            @Value("${procurax.outbox.batch-size:25}") int batchSize,
                            @Value("${procurax.outbox.max-attempts:8}") int maxAttempts,
                            @Value("${procurax.outbox.retry-delay-seconds:15}") long retryDelaySeconds,
                            @Value("${procurax.outbox.send-timeout-seconds:5}") long sendTimeoutSeconds,
                            @Value("${procurax.outbox.poll-interval-ms:1000}") long pollIntervalMillis) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.retryDelaySeconds = retryDelaySeconds;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalArgumentException("Outbox batch size must be between 1 and 500");
        }
        if (maxAttempts < 1 || maxAttempts > 100) {
            throw new IllegalArgumentException("Outbox max attempts must be between 1 and 100");
        }
        if (retryDelaySeconds < 1 || retryDelaySeconds > 3600) {
            throw new IllegalArgumentException("Outbox retry delay must be between 1 and 3600 seconds");
        }
        if (sendTimeoutSeconds < 1 || sendTimeoutSeconds > 60) {
            throw new IllegalArgumentException("Outbox send timeout must be between 1 and 60 seconds");
        }
        if (pollIntervalMillis < 100 || pollIntervalMillis > 300_000) {
            throw new IllegalArgumentException("Outbox poll interval must be between 100 and 300000 milliseconds");
        }
    }

    @Scheduled(fixedDelayString = "${procurax.outbox.poll-interval-ms:1000}")
    @Transactional
    public void dispatch() {
        List<OutboxRecord> due = jdbc.query("""
                SELECT event.id, event.organization_id, event.aggregate_type, event.aggregate_id,
                       event.event_type, event.topic, event.correlation_id, event.causation_id,
                       event.payload::text, event.retry_count, event.created_at
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
                LIMIT ?
                FOR UPDATE OF event SKIP LOCKED
                """, (row, index) -> new OutboxRecord(
                        row.getObject("id", UUID.class),
                        row.getObject("organization_id", UUID.class),
                        row.getString("aggregate_type"),
                        row.getObject("aggregate_id", UUID.class),
                        row.getString("event_type"),
                        row.getString("topic"),
                        row.getObject("correlation_id", UUID.class),
                        row.getObject("causation_id", UUID.class),
                        row.getString("payload"),
                        row.getInt("retry_count"),
                        row.getTimestamp("created_at").toInstant()), batchSize);
        for (OutboxRecord event : due) {
            if (event.retryCount() >= maxAttempts) {
                deadLetter(event);
            } else {
                publish(event);
            }
        }
    }

    private void publish(OutboxRecord event) {
        try {
            kafka.send(event.topic(), event.aggregateId().toString(), envelope(event))
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            jdbc.update("""
                    UPDATE outbox_events
                    SET status = 'PUBLISHED', published_at = now(), last_attempt_at = now(),
                        last_error = NULL
                    WHERE id = ? AND status = 'PENDING'
                    """, event.id());
        } catch (Exception exception) {
            String error = errorMessage(exception);
            int nextAttempt = event.retryCount() + 1;
            long delay = retryDelay(nextAttempt);
            jdbc.update("""
                    UPDATE outbox_events
                    SET retry_count = ?, last_attempt_at = now(), last_error = ?,
                        next_attempt_at = now() + (? * interval '1 second')
                    WHERE id = ? AND status = 'PENDING'
                    """, nextAttempt, error, delay, event.id());
            log.warn("Kafka publish failed for outbox event {} (attempt {} of {}): {}",
                    event.id(), nextAttempt, maxAttempts, error);
        }
    }

    private void deadLetter(OutboxRecord event) {
        try {
            kafka.send(event.topic() + ".DLT", event.aggregateId().toString(), envelope(event))
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            jdbc.update("""
                    UPDATE outbox_events
                    SET status = 'DEAD_LETTERED', dead_lettered_at = now(), last_attempt_at = now()
                    WHERE id = ? AND status = 'PENDING'
                    """, event.id());
            log.error("Outbox event {} exhausted retries and was sent to {}.DLT",
                    event.id(), event.topic());
        } catch (Exception exception) {
            String error = errorMessage(exception);
            long delay = retryDelay(event.retryCount() + 1);
            jdbc.update("""
                    UPDATE outbox_events
                    SET retry_count = retry_count + 1, last_attempt_at = now(), last_error = ?,
                        next_attempt_at = now() + (? * interval '1 second')
                    WHERE id = ? AND status = 'PENDING'
                    """, error, delay, event.id());
            log.error("Failed to publish exhausted outbox event {} to its dead-letter topic: {}",
                    event.id(), error);
        }
    }

    private long retryDelay(int attempt) {
        int exponent = Math.min(Math.max(attempt - 1, 0), 20);
        return Math.min(3600, retryDelaySeconds * (1L << exponent));
    }

    private String envelope(OutboxRecord event) {
        try {
            return objectMapper.writeValueAsString(new EventEnvelope(event.id(), event.eventType(),
                    event.createdAt(), event.organizationId(), event.aggregateType(), event.aggregateId(),
                    event.correlationId(), event.causationId(), objectMapper.readTree(event.payload())));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored outbox event cannot be serialized: " + event.id(), exception);
        }
    }

    private String errorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    private record OutboxRecord(UUID id, UUID organizationId, String aggregateType, UUID aggregateId,
                                String eventType, String topic, UUID correlationId, UUID causationId,
                                String payload, int retryCount, Instant createdAt) {
    }

    private record EventEnvelope(UUID eventId, String eventType, Instant occurredAt, UUID organizationId,
                                 String aggregateType, UUID aggregateId, UUID correlationId, UUID causationId,
                                 Object payload) {
    }
}
