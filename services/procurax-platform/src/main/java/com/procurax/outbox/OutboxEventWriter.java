package com.procurax.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procurax.common.correlation.CorrelationIdFilter;
import com.procurax.identity.security.OrganizationContext;
import com.procurax.identity.security.SecurityPrincipal;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventWriter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final OrganizationContext organizationContext;

    public OutboxEventWriter(JdbcTemplate jdbc, ObjectMapper objectMapper,
                             OrganizationContext organizationContext) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.organizationContext = organizationContext;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID record(String aggregateType, UUID aggregateId, UUID organizationId,
                       String eventType, String topic, UUID correlationId, Object payload) {
        return record(aggregateType, aggregateId, organizationId, eventType, topic,
                correlationId, currentActorId(), payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID record(String aggregateType, UUID aggregateId, UUID organizationId,
                       String eventType, String topic, UUID correlationId, UUID actorId, Object payload) {
        UUID eventId = UUID.randomUUID();
        String json = toJson(payload);
        jdbc.update("""
                INSERT INTO outbox_events
                    (id, organization_id, aggregate_type, aggregate_id, event_type, topic,
                     correlation_id, payload)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """, eventId, organizationId, aggregateType, aggregateId, eventType, topic,
                correlationId, json);

        jdbc.update("""
                INSERT INTO audit_events
                    (id, organization_id, actor_type, actor_id, action, resource_type, resource_id,
                     correlation_id, event_id, details)
                VALUES (?, ?, 'USER', ?, ?, ?, ?, ?, ?, ?::jsonb)
                """, UUID.randomUUID(), organizationId, actorId.toString(), eventType,
                aggregateType, aggregateId.toString(), correlationId, eventId, json);
        return eventId;
    }

    private UUID currentActorId() {
        try {
            SecurityPrincipal principal = organizationContext.currentPrincipal();
            return principal.getUserId();
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            throw new IllegalStateException("A business event requires an authenticated actor", exception);
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Business event payload cannot be serialized", exception);
        }
    }

    public static UUID currentCorrelationId(UUID fallback) {
        String current = CorrelationIdFilter.current();
        return current == null ? fallback : UUID.fromString(current);
    }
}
