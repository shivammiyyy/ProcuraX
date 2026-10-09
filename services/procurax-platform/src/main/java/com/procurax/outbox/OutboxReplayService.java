package com.procurax.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurax.common.error.BusinessException;
import com.procurax.identity.security.OrganizationContext;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxReplayService {

    private static final int MAX_REPLAYS = 3;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final OrganizationContext organizationContext;

    public OutboxReplayService(JdbcTemplate jdbc, ObjectMapper objectMapper,
                               OrganizationContext organizationContext) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.organizationContext = organizationContext;
    }

    @Transactional
    public OutboxReplayResponse replay(UUID eventId, String reason) {
        UUID organizationId = organizationContext.currentOrganizationId();
        UUID actorId = organizationContext.currentPrincipal().getUserId();
        ReplayableEvent event = jdbc.query("""
                SELECT replay_count
                FROM outbox_events
                WHERE id = ? AND organization_id = ? AND status = 'DEAD_LETTERED'
                FOR UPDATE
                """, rows -> rows.next()
                        ? new ReplayableEvent(rows.getInt("replay_count"))
                        : null, eventId, organizationId);
        if (event == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "OUTBOX_EVENT_NOT_FOUND",
                    "Dead-lettered event not found");
        }
        if (event.replayCount() >= MAX_REPLAYS) {
            throw new BusinessException(HttpStatus.CONFLICT, "OUTBOX_REPLAY_LIMIT_REACHED",
                    "The event has reached its replay limit");
        }

        int nextReplayCount = event.replayCount() + 1;
        Instant nextAttemptAt = Instant.now();
        int updated = jdbc.update("""
                UPDATE outbox_events
                SET status = 'PENDING', retry_count = 0, replay_count = ?,
                    next_attempt_at = now(), published_at = NULL, dead_lettered_at = NULL
                WHERE id = ? AND organization_id = ? AND status = 'DEAD_LETTERED'
                """, nextReplayCount, eventId, organizationId);
        if (updated != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "OUTBOX_EVENT_STATE_CHANGED",
                    "The event could not be replayed in its current state");
        }

        jdbc.update("""
                INSERT INTO audit_events
                    (id, organization_id, actor_type, actor_id, action, resource_type, resource_id,
                     correlation_id, details)
                VALUES (?, ?, 'USER', ?, 'OUTBOX_EVENT_REPLAY_REQUESTED', 'OUTBOX_EVENT', ?, ?, ?::jsonb)
                """, UUID.randomUUID(), organizationId, actorId.toString(), eventId.toString(),
                eventId, details(reason, nextReplayCount));
        return new OutboxReplayResponse(eventId, "PENDING", nextReplayCount, nextAttemptAt);
    }

    private String details(String reason, int replayCount) {
        try {
            return objectMapper.writeValueAsString(Map.of("reason", reason, "replayCount", replayCount));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Outbox replay audit details cannot be serialized", exception);
        }
    }

    private record ReplayableEvent(int replayCount) {
    }
}
