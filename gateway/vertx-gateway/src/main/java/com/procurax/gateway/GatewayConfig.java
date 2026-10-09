package com.procurax.gateway;

import java.net.URI;

public record GatewayConfig(int port, URI backendUri, int connectTimeoutMillis,
                            int rateLimitRequests, long rateLimitWindowMillis,
                            int maxTrackedClients) {

    public GatewayConfig {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Gateway port must be between 0 and 65535");
        }
        if (backendUri == null || !("http".equalsIgnoreCase(backendUri.getScheme())
                || "https".equalsIgnoreCase(backendUri.getScheme()))
                || backendUri.getHost() == null || backendUri.getUserInfo() != null
                || backendUri.getQuery() != null || backendUri.getFragment() != null
                || (backendUri.getRawPath() != null && !backendUri.getRawPath().isEmpty()
                    && !"/".equals(backendUri.getRawPath()))) {
            throw new IllegalArgumentException("Backend URL must be an HTTP(S) origin without a path");
        }
        if (connectTimeoutMillis < 1) {
            throw new IllegalArgumentException("Backend connect timeout must be positive");
        }
        if (rateLimitRequests < 1 || rateLimitRequests > 100_000) {
            throw new IllegalArgumentException("Rate limit must be between 1 and 100000 requests");
        }
        if (rateLimitWindowMillis < 100 || rateLimitWindowMillis > 3_600_000) {
            throw new IllegalArgumentException("Rate-limit window must be between 100 and 3600000 milliseconds");
        }
        if (maxTrackedClients < 1 || maxTrackedClients > 1_000_000) {
            throw new IllegalArgumentException("Tracked client limit must be between 1 and 1000000");
        }
    }

    public static GatewayConfig fromEnvironment() {
        int port = integerEnvironment("GATEWAY_PORT", 8080);
        int timeout = integerEnvironment("GATEWAY_CONNECT_TIMEOUT_MS", 5000);
        int rateLimit = integerEnvironment("GATEWAY_RATE_LIMIT_REQUESTS", 120);
        long rateWindowMillis = longEnvironment("GATEWAY_RATE_LIMIT_WINDOW_MS", 60_000);
        int maxTrackedClients = integerEnvironment("GATEWAY_MAX_TRACKED_CLIENTS", 20_000);
        String backend = System.getenv().getOrDefault("BACKEND_URL", "http://localhost:8081");
        return new GatewayConfig(port, URI.create(backend), timeout,
                rateLimit, rateWindowMillis, maxTrackedClients);
    }

    private static int integerEnvironment(String name, int fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    private static long longEnvironment(String name, long fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    public boolean backendUsesTls() {
        return "https".equalsIgnoreCase(backendUri.getScheme());
    }

    public int backendPort() {
        return backendUri.getPort() == -1 ? (backendUsesTls() ? 443 : 80) : backendUri.getPort();
    }

    public String backendHost() {
        return backendUri.getHost();
    }
}
