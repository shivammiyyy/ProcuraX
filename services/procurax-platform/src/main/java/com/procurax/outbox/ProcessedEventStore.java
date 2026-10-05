package com.procurax.outbox;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProcessedEventStore {

    private final JdbcTemplate jdbc;

    public ProcessedEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public boolean claim(String consumer, UUID eventId) {
        if (consumer == null || !consumer.matches("[a-zA-Z0-9._-]{1,100}")) {
            throw new IllegalArgumentException("Consumer name must be 1-100 safe identifier characters");
        }
        return jdbc.update("""
                INSERT INTO processed_events (consumer, event_id)
                VALUES (?, ?)
                ON CONFLICT (consumer, event_id) DO NOTHING
                """, consumer, eventId) == 1;
    }
}
