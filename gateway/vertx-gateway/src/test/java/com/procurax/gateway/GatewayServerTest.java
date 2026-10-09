package com.procurax.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GatewayServerTest {

    private Vertx vertx;
    private HttpServer upstream;
    private HttpServer gatewayServer;
    private GatewayServer gateway;
    private HttpClient client;

    @BeforeEach
    void startServers() throws Exception {
        vertx = Vertx.vertx();
        upstream = vertx.createHttpServer().requestHandler(request ->
                request.body().onSuccess(body -> request.response()
                        .setStatusCode(202)
                        .putHeader("Set-Cookie", "session=upstream; HttpOnly")
                        .putHeader("Connection", "X-Upstream-Hop")
                        .putHeader("X-Upstream-Hop", "must-not-leak")
                        .end(String.join("|",
                                request.path(), request.query() == null ? "" : request.query(),
                                String.valueOf(request.getHeader("Cookie")),
                                String.valueOf(request.getHeader("X-Correlation-ID")),
                                String.valueOf(request.getHeader("X-Forwarded-For")),
                                String.valueOf(request.getHeader("X-Forwarded-Proto")),
                                String.valueOf(request.getHeader("Forwarded")),
                                String.valueOf(request.getHeader("X-Client-Secret")),
                                body.toString()))));
        upstream.listen(0, "127.0.0.1").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
        gateway = new GatewayServer(vertx, new GatewayConfig(0,
                URI.create("http://127.0.0.1:" + upstream.actualPort()), 2000, 2, 60_000, 100));
        gatewayServer = gateway.start().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }

    @AfterEach
    void stopServers() throws Exception {
        if (gateway != null) {
            gateway.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void streamsRequestsAndResponsesWhileReplacingUntrustedForwardingHeaders() throws Exception {
        UUID correlationId = UUID.randomUUID();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + gatewayServer.actualPort()
                        + "/api/v1/test?filter=active"))
                .timeout(Duration.ofSeconds(10))
                .header("Cookie", "SESSION=abc; XSRF-TOKEN=token")
                .header("X-Correlation-ID", correlationId.toString())
                .header("X-Forwarded-For", "203.0.113.10")
                .header("X-Forwarded-Proto", "https")
                .header("Forwarded", "for=198.51.100.10;proto=https")
                .header("X-Client-Secret", "preserved")
                .POST(HttpRequest.BodyPublishers.ofString("request-body"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(202, response.statusCode());
        assertEquals("1", response.headers().firstValue("X-RateLimit-Remaining").orElseThrow());
        assertEquals(correlationId.toString(), response.headers().firstValue("X-Correlation-ID").orElseThrow());
        assertEquals("session=upstream; HttpOnly", response.headers().firstValue("Set-Cookie").orElseThrow());
        assertTrue(response.headers().firstValue("X-Upstream-Hop").isEmpty());
        assertTrue(response.body().contains("/api/v1/test|filter=active"));
        assertTrue(response.body().contains("SESSION=abc; XSRF-TOKEN=token"));
        assertTrue(response.body().contains(correlationId.toString()));
        assertTrue(response.body().contains("127.0.0.1"));
        assertTrue(response.body().contains("|http|null|preserved|request-body"));
    }

    @Test
    void healthEndpointDoesNotRequireAnUpstream() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + gatewayServer.actualPort() + "/health"))
                .timeout(Duration.ofSeconds(10))
                .GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"UP\"}", response.body());
        assertTrue(response.headers().firstValue("X-Correlation-ID").isPresent());
    }

    @Test
    void rateLimitsPeerAddressAndDoesNotTrustForwardedForOverrides() throws Exception {
        HttpResponse<String> first = get("/api/v1/first", "198.51.100.1");
        HttpResponse<String> second = get("/api/v1/second", "198.51.100.2");
        HttpResponse<String> health = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + gatewayServer.actualPort() + "/health"))
                        .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> blocked = get("/api/v1/third", "198.51.100.3");

        assertEquals(202, first.statusCode());
        assertEquals(202, second.statusCode());
        assertEquals("0", second.headers().firstValue("X-RateLimit-Remaining").orElseThrow());
        assertEquals(200, health.statusCode());
        assertEquals(429, blocked.statusCode());
        assertTrue(blocked.body().contains("\"code\":\"RATE_LIMITED\""));
        assertEquals("60", blocked.headers().firstValue("Retry-After").orElseThrow());
        assertTrue(blocked.headers().firstValue("X-Correlation-ID").isPresent());
    }

    private HttpResponse<String> get(String path, String forwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + gatewayServer.actualPort() + path))
                .timeout(Duration.ofSeconds(10))
                .header("X-Forwarded-For", forwardedFor)
                .GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
