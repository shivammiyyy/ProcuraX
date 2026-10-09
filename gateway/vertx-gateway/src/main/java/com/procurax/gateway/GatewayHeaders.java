package com.procurax.gateway;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class GatewayHeaders {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade", "host");

    private GatewayHeaders() {
    }

    static UUID correlationId(String candidate) {
        if (candidate != null) {
            try {
                return UUID.fromString(candidate.trim());
            } catch (IllegalArgumentException ignored) {
                // Generate a new ID instead of echoing malformed input.
            }
        }
        return UUID.randomUUID();
    }

    static void copyRequestHeaders(HttpServerRequest source, MultiMap target, UUID correlationId) {
        Set<String> excluded = new HashSet<>(HOP_BY_HOP_HEADERS);
        excluded.add("x-correlation-id");
        excluded.add("x-forwarded-for");
        excluded.add("x-forwarded-host");
        excluded.add("x-forwarded-port");
        excluded.add("x-forwarded-prefix");
        excluded.add("x-forwarded-proto");
        excluded.add("x-forwarded-server");
        excluded.add("x-forwarded-ssl");
        excluded.add("x-real-ip");
        excluded.add("forwarded");
        excluded.add("x-original-host");
        excluded.add("x-original-url");
        excluded.add("x-rewrite-url");
        excluded.add("expect");
        String connectionHeaders = source.getHeader("Connection");
        if (connectionHeaders != null) {
            for (String header : connectionHeaders.split(",")) {
                excluded.add(header.trim().toLowerCase(Locale.ROOT));
            }
        }
        source.headers().forEach(entry -> {
            if (!excluded.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                target.add(entry.getKey(), entry.getValue());
            }
        });
        target.set("X-Correlation-ID", correlationId.toString());
        if (source.remoteAddress() != null && source.remoteAddress().host() != null) {
            target.set("X-Forwarded-For", source.remoteAddress().host());
        }
        target.set("X-Forwarded-Proto", source.isSSL() ? "https" : "http");
    }

    static void copyResponseHeaders(MultiMap source, MultiMap target) {
        Set<String> excluded = new HashSet<>(HOP_BY_HOP_HEADERS);
        String connectionHeaders = source.get("Connection");
        if (connectionHeaders != null) {
            for (String header : connectionHeaders.split(",")) {
                excluded.add(header.trim().toLowerCase(Locale.ROOT));
            }
        }
        source.forEach(entry -> {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            if (!excluded.contains(name)) {
                target.add(entry.getKey(), entry.getValue());
            }
        });
    }
}
