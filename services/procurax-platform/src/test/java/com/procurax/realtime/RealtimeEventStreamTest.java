package com.procurax.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class RealtimeEventStreamTest {

    @Test
    void publishesOnlyToTheMatchingOrganizationAndCleansUpConnections() {
        RealtimeEventStream stream = new RealtimeEventStream(10, 10, 2, 1);
        UUID subscribedOrganization = UUID.randomUUID();
        UUID otherOrganization = UUID.randomUUID();
        SseEmitter emitter = stream.open(subscribedOrganization);
        RealtimeEvent event = new RealtimeEvent(UUID.randomUUID(), "RFQ_PUBLISHED", Instant.now(), "RFQ",
                UUID.randomUUID());

        assertThat(stream.publish(otherOrganization, event)).isZero();
        assertThat(stream.publish(subscribedOrganization, event)).isEqualTo(1);
        assertThat(stream.activeClients()).isEqualTo(1);

        emitter.complete();
        stream.closeAll();
        assertThat(stream.activeClients()).isZero();
    }

    @Test
    void enforcesGlobalAndOrganizationConnectionCaps() {
        RealtimeEventStream stream = new RealtimeEventStream(1, 1, 2, 1);
        UUID organizationId = UUID.randomUUID();
        SseEmitter emitter = stream.open(organizationId);

        assertThatThrownBy(() -> stream.open(organizationId))
                .isInstanceOf(ResponseStatusException.class);
        emitter.complete();
        stream.closeAll();
        assertThat(stream.activeClients()).isZero();
    }

    @Test
    void kafkaEnvelopeIsRoutedByItsOrganizationWithoutForwardingItsPayload() {
        RealtimeEventStream stream = new RealtimeEventStream(10, 10, 2, 1);
        RealtimeEventListener listener = new RealtimeEventListener(new ObjectMapper(), stream);
        UUID subscribedOrganization = UUID.randomUUID();
        SseEmitter emitter = stream.open(subscribedOrganization);
        String eventId = UUID.randomUUID().toString();
        String aggregateId = UUID.randomUUID().toString();
        String envelope = """
                {"eventId":"%s","eventType":"RFQ_PUBLISHED","occurredAt":"2025-01-01T00:00:00Z",
                 "organizationId":"%s","aggregateType":"RFQ","aggregateId":"%s",
                 "payload":{"sensitive":"not for the realtime stream"}}
                """.formatted(eventId, subscribedOrganization, aggregateId);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("procurax.rfq.v1", 0, 1L, null, envelope);

        assertThat(listener.process(record)).isEqualTo(1);
        assertThat(listener.process(new ConsumerRecord<>("procurax.rfq.v1", 0, 2L, null,
                envelope.replace(subscribedOrganization.toString(), UUID.randomUUID().toString())))).isZero();

        emitter.complete();
        stream.closeAll();
        assertThat(stream.activeClients()).isZero();
    }
}
