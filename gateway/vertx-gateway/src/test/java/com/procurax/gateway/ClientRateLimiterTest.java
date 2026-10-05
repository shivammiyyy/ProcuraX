package com.procurax.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientRateLimiterTest {

    @Test
    void enforcesWindowAndAllowsRequestsAfterExpiry() {
        ClientRateLimiter limiter = new ClientRateLimiter(2, 1000, 10);

        assertTrue(limiter.acquire("client", 0).allowed());
        assertTrue(limiter.acquire("client", 100).allowed());
        ClientRateLimiter.Decision blocked = limiter.acquire("client", 200);
        assertFalse(blocked.allowed());
        assertEquals(1, blocked.retryAfterSeconds());
        assertTrue(limiter.acquire("client", 1_000_000_000L).allowed());
    }

    @Test
    void rejectsNewKeysWhenCapacityIsFullAndReclaimsExpiredWindows() {
        ClientRateLimiter limiter = new ClientRateLimiter(1, 1000, 1);

        assertTrue(limiter.acquire("first", 0).allowed());
        assertFalse(limiter.acquire("second", 100).allowed());
        assertTrue(limiter.acquire("second", 1_000_000_000L).allowed());
    }
}
