package com.procurax.gateway;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import java.util.UUID;

public final class GatewayServer {

    private final Vertx vertx;
    private final GatewayConfig config;
    private final HttpClient backendClient;
    private final ClientRateLimiter rateLimiter;
    private long cleanupTimerId = -1;

    public GatewayServer(Vertx vertx, GatewayConfig config) {
        this.vertx = vertx;
        this.config = config;
        this.backendClient = vertx.createHttpClient(new HttpClientOptions()
                .setSsl(config.backendUsesTls())
                .setConnectTimeout(config.connectTimeoutMillis())
                .setKeepAlive(true));
        this.rateLimiter = new ClientRateLimiter(config.rateLimitRequests(),
                config.rateLimitWindowMillis(), config.maxTrackedClients());
    }

    public Future<HttpServer> start() {
        cleanupTimerId = vertx.setPeriodic(config.rateLimitWindowMillis(), ignored ->
                rateLimiter.prune(System.nanoTime()));
        return vertx.createHttpServer()
                .requestHandler(this::handle)
                .listen(config.port(), "0.0.0.0")
                .onFailure(error -> vertx.cancelTimer(cleanupTimerId));
    }

    public Future<Void> close() {
        if (cleanupTimerId != -1) {
            vertx.cancelTimer(cleanupTimerId);
        }
        return backendClient.close();
    }

    private void handle(HttpServerRequest request) {
        UUID correlationId = GatewayHeaders.correlationId(request.getHeader("X-Correlation-ID"));
        if ("/health".equals(request.path())) {
            request.response()
                    .putHeader("Content-Type", "application/json")
                    .putHeader("X-Correlation-ID", correlationId.toString())
                    .end("{\"status\":\"UP\"}");
            return;
        }
        String clientKey = request.remoteAddress() == null
                ? "unknown" : request.remoteAddress().host();
        ClientRateLimiter.Decision decision = rateLimiter.acquire(clientKey, System.nanoTime());
        if (!decision.allowed()) {
            request.response().setStatusCode(429)
                    .putHeader("Content-Type", "application/json")
                    .putHeader("X-Correlation-ID", correlationId.toString())
                    .putHeader("X-RateLimit-Limit", String.valueOf(decision.limit()))
                    .putHeader("X-RateLimit-Remaining", "0")
                    .putHeader("X-RateLimit-Reset-After", String.valueOf(decision.resetAfterSeconds()))
                    .putHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()))
                    .end("{\"status\":429,\"code\":\"RATE_LIMITED\","
                            + "\"message\":\"Too many requests\"}");
            return;
        }
        if (request.method() == HttpMethod.CONNECT || request.method() == HttpMethod.TRACE) {
            request.response().setStatusCode(405).putHeader("Allow", "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS")
                    .end();
            return;
        }
        proxy(request, correlationId, decision);
    }

    private void proxy(HttpServerRequest inbound, UUID correlationId, ClientRateLimiter.Decision decision) {
        String requestUri = inbound.uri();
        if (!requestUri.startsWith("/") || requestUri.startsWith("//")) {
            inbound.response().setStatusCode(400).end();
            return;
        }
        inbound.pause();
        if ("100-continue".equalsIgnoreCase(inbound.getHeader("Expect"))) {
            inbound.response().writeContinue();
        }
        HttpMethod method = inbound.method();
        backendClient.request(method, config.backendPort(), config.backendHost(), requestUri)
                .onFailure(error -> {
                    inbound.resume();
                    gatewayError(inbound.response(), correlationId);
                })
                .onSuccess(outbound -> forward(inbound, outbound, correlationId, decision));
    }

    private void forward(HttpServerRequest inbound, HttpClientRequest outbound, UUID correlationId,
                         ClientRateLimiter.Decision decision) {
        HttpServerResponse downstream = inbound.response();
        GatewayHeaders.copyRequestHeaders(inbound, outbound.headers(), correlationId);
        if (inbound.getHeader("Content-Length") == null) {
            outbound.setChunked(true);
        }
        outbound.exceptionHandler(error -> {
            if (!downstream.ended()) {
                gatewayError(downstream, correlationId);
            }
        });
        outbound.response()
                .onFailure(error -> gatewayError(downstream, correlationId))
                .onSuccess(upstream -> {
                    downstream.setStatusCode(upstream.statusCode());
                    GatewayHeaders.copyResponseHeaders(upstream.headers(), downstream.headers());
                    downstream.putHeader("X-Correlation-ID", correlationId.toString());
                    downstream.putHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
                    downstream.putHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
                    downstream.putHeader("X-RateLimit-Reset-After", String.valueOf(decision.resetAfterSeconds()));
                    upstream.pipeTo(downstream).onFailure(error -> {
                        if (!downstream.ended()) {
                            downstream.reset();
                        }
                    });
                });

        inbound.pipeTo(outbound).onFailure(error -> {
            if (!downstream.ended()) {
                gatewayError(downstream, correlationId);
            }
        });
    }

    private void gatewayError(HttpServerResponse response, UUID correlationId) {
        if (!response.ended()) {
            response.setStatusCode(502)
                    .putHeader("Content-Type", "application/json")
                    .putHeader("X-Correlation-ID", correlationId.toString())
                    .end("{\"status\":502,\"code\":\"UPSTREAM_UNAVAILABLE\","
                            + "\"message\":\"The application service is unavailable\"}");
        }
    }
}
