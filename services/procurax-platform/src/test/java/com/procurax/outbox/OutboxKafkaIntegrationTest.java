package com.procurax.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OutboxKafkaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17");

    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void pendingEventIsPublishedToBrokerAsEnvelopeAndMarkedPublished() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String topic = "procurax.it-" + UUID.randomUUID() + ".v1";
        insert(eventId, aggregateId, organizationId, topic, 0);

        dispatcher().dispatch();

        ConsumerRecord<String, String> record = consumeOne(topic);
        assertThat(record.key()).isEqualTo(aggregateId.toString());
        JsonNode body = objectMapper.readTree(record.value());
        assertThat(body.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(body.get("organizationId").asText()).isEqualTo(organizationId.toString());
        assertThat(body.path("payload").path("value").asInt()).isEqualTo(7);
        assertThat(status(eventId)).isEqualTo("PUBLISHED");
    }

    @Test
    void exhaustedEventIsDeliveredToDeadLetterTopic() throws Exception {
        UUID eventId = UUID.randomUUID();
        String topic = "procurax.it-" + UUID.randomUUID() + ".v1";
        insert(eventId, UUID.randomUUID(), UUID.randomUUID(), topic, 8);

        dispatcher().dispatch();

        ConsumerRecord<String, String> record = consumeOne(topic + ".DLT");
        assertThat(objectMapper.readTree(record.value()).get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(status(eventId)).isEqualTo("DEAD_LETTERED");
    }

    private OutboxDispatcher dispatcher() {
        return new OutboxDispatcher(jdbc, kafkaTemplate, objectMapper, 25, 8, 15, 10, 1000);
    }

    private void insert(UUID id, UUID aggregateId, UUID organizationId, String topic, int retryCount) {
        jdbc.update("""
                INSERT INTO outbox_events
                    (id, organization_id, aggregate_type, aggregate_id, event_type, topic, correlation_id,
                     payload, retry_count)
                VALUES (?, ?, 'IT_TEST', ?, 'IT_EVENT', ?, ?, '{"value":7}'::jsonb, ?)
                """, id, organizationId, aggregateId, topic, UUID.randomUUID(), retryCount);
    }

    private String status(UUID id) {
        return jdbc.queryForObject("SELECT status FROM outbox_events WHERE id = ?", String.class, id);
    }

    private ConsumerRecord<String, String> consumeOne(String topic) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    return record;
                }
            }
        }
        throw new AssertionError("No record received on " + topic);
    }
}
