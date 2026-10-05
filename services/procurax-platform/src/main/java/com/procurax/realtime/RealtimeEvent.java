package com.procurax.realtime;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent(UUID eventId, String eventType, Instant occurredAt,
                            String aggregateType, UUID aggregateId) {
}
