package com.procurax.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxReplayResponse(
        UUID eventId,
        String status,
        int replayCount,
        Instant nextAttemptAt) {
}
