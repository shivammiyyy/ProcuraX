package com.procurax.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RealtimeEventListener {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventListener.class);
    private final ObjectMapper objectMapper;
    private final RealtimeEventStream eventStream;

    public RealtimeEventListener(ObjectMapper objectMapper, RealtimeEventStream eventStream) {
        this.objectMapper = objectMapper;
        this.eventStream = eventStream;
    }

    @KafkaListener(
            topicPattern = "${procurax.realtime.topic-pattern}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${procurax.realtime.enabled:true}")
    public void onEvent(ConsumerRecord<String, String> record) {
        process(record);
    }

    int process(ConsumerRecord<String, String> record) {
        try {
            if (record.value() == null) {
                throw new IllegalArgumentException("Empty event envelope");
            }
            JsonNode envelope = objectMapper.readTree(record.value());
            RealtimeEvent event = new RealtimeEvent(
                    UUID.fromString(requiredText(envelope, "eventId")),
                    requiredText(envelope, "eventType"),
                    Instant.parse(requiredText(envelope, "occurredAt")),
                    requiredText(envelope, "aggregateType"),
                    UUID.fromString(requiredText(envelope, "aggregateId")));
            String organizationId = requiredText(envelope, "organizationId");
            if (!event.eventType().matches("[A-Z][A-Z0-9_]{0,79}")
                    || !event.aggregateType().matches("[A-Z][A-Z0-9_]{0,49}")) {
                throw new IllegalArgumentException("Invalid event metadata");
            }
            return eventStream.publish(UUID.fromString(organizationId), event);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("Ignoring malformed realtime event from topic {} partition {} offset {} ({})",
                    record.topic(), record.partition(), record.offset(), exception.getClass().getSimpleName());
            return 0;
        }
    }

    private String requiredText(JsonNode envelope, String field) {
        JsonNode value = envelope.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Missing event field");
        }
        return value.textValue();
    }
}
