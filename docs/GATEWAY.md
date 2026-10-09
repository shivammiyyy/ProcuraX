# Vert.x gateway

The gateway in `gateway/vertx-gateway/` is a thin HTTP edge in front of Spring Boot. It does not authenticate users or make authorization decisions; Spring's session, CSRF, tenant context and permission checks remain authoritative.

## Local entry point

With Docker Compose running:

- Gateway: `http://localhost:8088`
- Gateway health: `GET /health`
- Spring backend: internal to the Compose network only; host requests should pass through the gateway.

Set `GATEWAY_PORT` to change the gateway's host port and `BACKEND_URL` to set the upstream origin. The upstream URL must be an HTTP(S) origin without a path. The gateway uses the upstream host's TLS certificate verification when HTTPS is configured.

The local in-memory rate limiter allows 120 requests per peer IP per 60-second fixed window by default. Tune it with `GATEWAY_RATE_LIMIT_REQUESTS`, `GATEWAY_RATE_LIMIT_WINDOW_MS`, and `GATEWAY_MAX_TRACKED_CLIENTS`. A rejected request receives `429 RATE_LIMITED`, `Retry-After`, and rate-limit headers. `/health` is exempt. Client identity comes from the socket peer address; forwarding headers are never used as rate-limit identity. On deployments behind a load balancer, configure network-level client-IP preservation before relying on per-client quotas.

## Proxy behavior

- Streams request and response bodies and preserves the original method, path, query string, status and application headers.
- Preserves browser cookies and CSRF headers for the Spring session flow.
- Removes hop-by-hop headers and header names listed by the inbound `Connection` header.
- Replaces inbound `X-Correlation-ID` with a valid UUID (or generates one) and writes that value to the response.
- Replaces client-supplied `X-Forwarded-For`, `X-Forwarded-Host`, `X-Forwarded-Proto`, and `X-Real-IP` rather than trusting them.
- Returns a generic `502 UPSTREAM_UNAVAILABLE` response if Spring cannot be reached.

`GET /api/v1/events` is an authenticated Spring Server-Sent Events endpoint proxied by this gateway. Spring derives the organization from the authenticated session, requires `EVENT_STREAM_READ`, consumes the configured Kafka business-topic pattern, and fans out event metadata through bounded per-client queues. Client-supplied organization IDs are not used for subscription routing. Slow clients are disconnected when their queue fills; SSE is not replayable, so clients must refetch current state after reconnecting. Only event metadata is emitted, never the outbox payload. The Kafka group ID defaults to a hostname-specific value so each application instance receives events for its local SSE subscribers; any explicit production override must also be unique per instance.

Compose no longer publishes Spring directly; local host traffic uses the gateway. Production deployments must preserve this network isolation and expose only a TLS-terminating trusted edge. Spring forwarded-header trust must remain disabled unless the deployment explicitly constrains and authenticates its proxy chain. The per-instance in-memory rate limit is not shared across replicas; use a distributed limiter or enforce aggregate quotas at a trusted edge for production. TLS termination, graceful draining, and production observability remain future work. Moving Kafka fan-out to the gateway should wait until it can validate Spring sessions and organization membership without trusting client-supplied tenant IDs.
