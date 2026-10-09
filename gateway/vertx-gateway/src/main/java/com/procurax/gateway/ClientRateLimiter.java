package com.procurax.gateway;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class ClientRateLimiter {

    private final int limit;
    private final long windowNanos;
    private final int maxTrackedClients;
    private final Map<String, Window> windows = new HashMap<>();

    ClientRateLimiter(int limit, long windowMillis, int maxTrackedClients) {
        if (limit < 1 || windowMillis < 1 || maxTrackedClients < 1) {
            throw new IllegalArgumentException("Rate-limiter limits must be positive");
        }
        this.limit = limit;
        this.windowNanos = Math.multiplyExact(windowMillis, 1_000_000L);
        this.maxTrackedClients = maxTrackedClients;
    }

    synchronized Decision acquire(String clientKey, long nowNanos) {
        Window window = windows.get(clientKey);
        if (window == null) {
            if (windows.size() >= maxTrackedClients) {
                prune(nowNanos);
                if (windows.size() >= maxTrackedClients) {
                    return new Decision(false, 1, 0, limit, 1);
                }
            }
            window = new Window(nowNanos, 0);
            windows.put(clientKey, window);
        } else if (nowNanos - window.startedAtNanos() >= windowNanos
                || nowNanos < window.startedAtNanos()) {
            window = new Window(nowNanos, 0);
            windows.put(clientKey, window);
        }

        if (window.requests() >= limit) {
            long remainingNanos = windowNanos - Math.max(0, nowNanos - window.startedAtNanos());
            long retryAfterSeconds = Math.max(1, (remainingNanos + 999_999_999L) / 1_000_000_000L);
            return new Decision(false, retryAfterSeconds, 0, limit, retryAfterSeconds);
        }

        Window updated = new Window(window.startedAtNanos(), window.requests() + 1);
        windows.put(clientKey, updated);
        long remainingNanos = windowNanos - Math.max(0, nowNanos - window.startedAtNanos());
        long resetAfterSeconds = Math.max(1, (remainingNanos + 999_999_999L) / 1_000_000_000L);
        return new Decision(true, 0, limit - updated.requests(), limit, resetAfterSeconds);
    }

    synchronized void prune(long nowNanos) {
        Iterator<Window> iterator = windows.values().iterator();
        while (iterator.hasNext()) {
            Window window = iterator.next();
            if (nowNanos < window.startedAtNanos()
                    || nowNanos - window.startedAtNanos() >= windowNanos) {
                iterator.remove();
            }
        }
    }

    record Decision(boolean allowed, long retryAfterSeconds, int remaining, int limit,
                    long resetAfterSeconds) {
    }

    private record Window(long startedAtNanos, int requests) {
    }
}
